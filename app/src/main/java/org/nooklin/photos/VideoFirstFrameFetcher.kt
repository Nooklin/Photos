package org.nooklin.photos

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class VideoFirstFrame(val source: Any, val startMs: Long = 0L)

// Must be the frame playback starts on, the system thumbnail causes a visible jump
class VideoFirstFrameFetcher(
    private val source: Any,
    private val startMs: Long,
    private val options: Options
) : Fetcher {
    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) { frameSlot.withPermit { fetchFrame() } }

    private fun fetchFrame(): FetchResult {
        val retriever = MediaMetadataRetriever()
        try {
            when (source) {
                is File -> retriever.setDataSource(source.absolutePath)
                is Uri -> retriever.setDataSource(options.context, source)
                else -> throw IOException("Unsupported video source")
            }
            // Trim start is rarely a keyframe, so read it exact
            val option = if (startMs > 0L) MediaMetadataRetriever.OPTION_CLOSEST else MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            val frame = retriever.getFrameAtTime(startMs * 1000L, option)
                ?: throw IOException("No frame at the start of the video")
            val image = scaleDown(uprightFrame(retriever, frame))
            return ImageFetchResult(
                image = image.asImage(),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        } finally {
            retriever.release()
        }
    }

    // Some devices apply the rotation, some don't: compare orientation
    private fun uprightFrame(retriever: MediaMetadataRetriever, frame: Bitmap): Bitmap {
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        val codedWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
        val codedHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
        if (rotation == 0 || codedWidth == null || codedHeight == null) return frame
        val codedLandscape = codedWidth >= codedHeight
        val expectedLandscape = if (rotation == 90 || rotation == 270) !codedLandscape else codedLandscape
        val frameLandscape = frame.width >= frame.height
        if (frameLandscape == expectedLandscape) return frame
        val rotated = Bitmap.createBitmap(
            frame, 0, 0, frame.width, frame.height,
            Matrix().apply { postRotate(rotation.toFloat()) },
            true
        )
        if (rotated !== frame) frame.recycle()
        return rotated
    }

    private fun scaleDown(source: Bitmap): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= MAX_EDGE) return source
        val scale = MAX_EDGE.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== source) source.recycle()
        return scaled
    }

    class Factory : Fetcher.Factory<VideoFirstFrame> {
        override fun create(data: VideoFirstFrame, options: Options, imageLoader: ImageLoader): Fetcher =
            VideoFirstFrameFetcher(data.source, data.startMs, options)
    }
}

private const val MAX_EDGE = 1600

// One decode at a time, several starved the photo decodes
private val frameSlot = Semaphore(1)
