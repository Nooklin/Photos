@file:OptIn(UnstableApi::class)

package org.nooklin.photos.ui.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Matrix
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.nooklin.photos.data.ImageNaming

private const val TAG = "VideoEditing"

// The edits of the video editor. The same values drive the live preview and the export, like the photo editor.
// Rotation is a multiple of 90 degrees, clockwise, as in the photo editor.
data class VideoEdits(
    val filter: PhotoFilter = PhotoFilter.NONE,
    val rotationDegrees: Float = 0f,
    val straightenDegrees: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropRect: CropRect = CropRect()
) {
    val hasCrop: Boolean
        get() = !(cropRect.left <= 0f && cropRect.top <= 0f && cropRect.right >= 1f && cropRect.bottom >= 1f)

    val isNeutral: Boolean
        get() = filter == PhotoFilter.NONE && rotationDegrees == 0f && straightenDegrees == 0f &&
            !flipHorizontal && !flipVertical && !hasCrop
}

// A photo filter is a 4x5 ColorMatrix (offsets in 0..255). The video effect takes a 4x4 matrix in column-major order.
// A video frame has alpha 1, so the offset column folds into the fourth column, and the result is the same colours.
fun colorMatrixToRgbMatrix(matrix: FloatArray): FloatArray {
    val out = FloatArray(16)
    for (row in 0..2) {
        for (col in 0..2) out[col * 4 + row] = matrix[row * 5 + col]
        out[3 * 4 + row] = matrix[row * 5 + 3] + matrix[row * 5 + 4] / 255f
    }
    out[15] = 1f
    return out
}

// The effects for the preview (live = true) or the export. Flip, straighten and the filter read the latest values
// from [live] on every frame, so dragging a slider needs no new effect list. Rotation is fixed in the list, a rotate
// tap builds a new list. Crop is only in the export: the preview shows the whole frame with the crop overlay on top.
fun buildVideoEffects(
    live: AtomicReference<VideoEdits>,
    displayWidth: Int,
    displayHeight: Int,
    forExport: Boolean
): List<Effect> {
    val edits = live.get()
    val effects = mutableListOf<Effect>()
    val rotation = ((edits.rotationDegrees % 360f) + 360f) % 360f
    // The photo editor turns clockwise, this effect turns counterclockwise
    if (rotation != 0f) {
        effects += ScaleAndRotateTransformation.Builder().setRotationDegrees(-rotation).build()
    }
    val swapped = rotation == 90f || rotation == 270f
    val frameWidth = if (swapped) displayHeight else displayWidth
    val frameHeight = if (swapped) displayWidth else displayHeight
    val aspect = if (frameHeight > 0) frameWidth.toFloat() / frameHeight.toFloat() else 1f

    if (!forExport || edits.flipHorizontal || edits.flipVertical) {
        effects += MatrixTransformation { _ ->
            val current = live.get()
            Matrix().apply {
                postScale(if (current.flipHorizontal) -1f else 1f, if (current.flipVertical) -1f else 1f)
            }
        }
    }
    if (!forExport || edits.straightenDegrees != 0f) {
        effects += MatrixTransformation { _ ->
            val degrees = live.get().straightenDegrees
            Matrix().apply {
                if (degrees != 0f) {
                    // Same fill scale as the photo editor: keep the frame size, no empty corners
                    val theta = Math.toRadians(degrees.toDouble())
                    val fill = (abs(cos(theta)) + abs(sin(theta)) * aspect).toFloat()
                    // The matrix works in -1..1 on both axes, so the rotation is done in a square space.
                    // Screen y points down in a photo and up here, so the angle changes sign.
                    postScale(aspect, 1f)
                    postRotate(-degrees)
                    postScale(1f / aspect, 1f)
                    postScale(fill, fill)
                }
            }
        }
    }
    if (!forExport || edits.filter != PhotoFilter.NONE) {
        effects += object : RgbMatrix {
            override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray =
                colorMatrixToRgbMatrix(live.get().filter.matrix)
        }
    }
    if (forExport && edits.hasCrop) {
        val crop = edits.cropRect
        effects += Crop(2f * crop.left - 1f, 2f * crop.right - 1f, 1f - 2f * crop.bottom, 1f - 2f * crop.top)
    }
    return effects
}

// Cuts and edits a video with Media3 Transformer, which decodes and encodes again (H.264).
// The file is written to the cache first and then added to the library as a new video.
object VideoEditExporter {
    suspend fun export(
        context: Context,
        source: Uri,
        startMs: Long,
        endMs: Long,
        edits: VideoEdits,
        displayWidth: Int,
        displayHeight: Int,
        onProgress: (Float) -> Unit
    ): Uri? {
        val appContext = context.applicationContext
        val directory = File(appContext.cacheDir, "video_export").apply { mkdirs() }
        val cacheFile = File(directory, "export_${System.currentTimeMillis()}.mp4")
        try {
            // Transformer must be used from one thread with a looper, the main thread
            val done = withContext(Dispatchers.Main) {
                runTransformer(appContext, source, startMs, endMs, edits, displayWidth, displayHeight, cacheFile, onProgress)
            }
            if (!done) return null
            return withContext(Dispatchers.IO) { copyToLibrary(appContext, cacheFile) }
        } finally {
            cacheFile.delete()
        }
    }

    // Same cut and edits, but the finished file stays in the cache and the caller deletes it. Null when it failed.
    suspend fun exportToFile(
        context: Context,
        source: Uri,
        startMs: Long,
        endMs: Long,
        edits: VideoEdits,
        displayWidth: Int,
        displayHeight: Int,
        onProgress: (Float) -> Unit
    ): File? {
        val appContext = context.applicationContext
        val directory = File(appContext.cacheDir, "video_export").apply { mkdirs() }
        val cacheFile = File(directory, "overwrite_${System.currentTimeMillis()}.mp4")
        val done = try {
            withContext(Dispatchers.Main) {
                runTransformer(appContext, source, startMs, endMs, edits, displayWidth, displayHeight, cacheFile, onProgress)
            }
        } catch (e: Exception) {
            cacheFile.delete()
            throw e
        }
        if (!done) {
            cacheFile.delete()
            return null
        }
        return cacheFile
    }

    private suspend fun runTransformer(
        context: Context,
        source: Uri,
        startMs: Long,
        endMs: Long,
        edits: VideoEdits,
        displayWidth: Int,
        displayHeight: Int,
        output: File,
        onProgress: (Float) -> Unit
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val handler = Handler(Looper.getMainLooper())
        var finished = false
        lateinit var transformer: Transformer
        val progressHolder = ProgressHolder()
        val poll = object : Runnable {
            override fun run() {
                if (finished) return
                if (transformer.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress(progressHolder.progress / 100f)
                }
                handler.postDelayed(this, 250)
            }
        }
        try {
            val mediaItem = MediaItem.Builder()
                .setUri(source)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build()
                )
                .build()
            val effects = buildVideoEffects(AtomicReference(edits), displayWidth, displayHeight, forExport = true)
            Log.d(TAG, "Export started: ${effects.size} effects, rotation ${edits.rotationDegrees}, straighten ${edits.straightenDegrees}, filter ${edits.filter}, size ${displayWidth}x$displayHeight")
            val editedItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(Effects(emptyList(), effects))
                .build()
            transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        Log.d(TAG, "Export done: ${exportResult.width}x${exportResult.height}")
                        finished = true
                        handler.removeCallbacks(poll)
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        Log.e(TAG, "Export failed, error code ${exportException.errorCodeName}", exportException)
                        finished = true
                        handler.removeCallbacks(poll)
                        if (continuation.isActive) continuation.resume(false)
                    }
                })
                .build()
            continuation.invokeOnCancellation {
                handler.post {
                    finished = true
                    handler.removeCallbacks(poll)
                    transformer.cancel()
                }
            }
            transformer.start(editedItem, output.absolutePath)
            handler.postDelayed(poll, 250)
        } catch (e: Exception) {
            Log.e(TAG, "Export could not start", e)
            finished = true
            if (continuation.isActive) continuation.resume(false)
        }
    }

    private fun copyToLibrary(context: Context, file: File): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, ImageNaming.videoFileName(context, "mp4"))
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val target = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            val stream = resolver.openOutputStream(target) ?: throw IllegalStateException("No file")
            stream.use { out -> file.inputStream().use { it.copyTo(out) } }
            resolver.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            target
        } catch (e: Exception) {
            Log.e(TAG, "Copy to the library failed", e)
            resolver.delete(target, null, null)
            null
        }
    }
}
