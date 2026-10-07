package org.nooklin.photos.ui.editor

import androidx.compose.ui.text.font.FontWeight
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Deblur
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vignette
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material.icons.filled.WbSunny
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import coil3.imageLoader
import coil3.memory.MemoryCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nooklin.photos.data.readExif
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.GlassPillContainer
import org.nooklin.photos.ui.common.detectPinchPanGestures
import java.io.File
import org.nooklin.photos.data.OriginalsStore
import android.widget.Toast
import org.nooklin.photos.data.MediaStoreRepository
import android.graphics.ImageDecoder
import android.content.ContentUris
import org.nooklin.photos.data.local.LocalMetadataStore

// Bottom bar (Styles, Adjust, Crop), same image box in every mode so the photo doesn't move
enum class EditorMode { STYLES, ADJUST, CROP }
enum class AdjustmentType(val label: String, val icon: ImageVector) {
    AUTO_ENHANCE("Auto", Icons.Filled.AutoFixHigh),
    EXPOSURE("Exposure", Icons.Filled.Exposure),
    BRIGHTNESS("Brightness", Icons.Filled.WbSunny),
    HIGHLIGHTS("Highlights", Icons.Filled.Highlight),
    SHADOWS("Shadows", Icons.Filled.DarkMode),
    CONTRAST("Contrast", Icons.Filled.Contrast),
    BLACK_POINT("Black Point", Icons.Filled.Tonality),
    SATURATION("Saturation", Icons.Filled.InvertColors),
    VIBRANCE("Vibrance", Icons.Filled.Palette),
    WARMTH("Warmth", Icons.Filled.WbIncandescent),
    TINT("Tint", Icons.Filled.Colorize),
    SHARPNESS("Sharpness", Icons.Filled.Deblur),
    DEFINITION("Definition", Icons.Filled.Texture),
    NOISE_REDUCTION("Noise Reduction", Icons.Filled.BlurOn),
    VIGNETTE("Vignette", Icons.Filled.Vignette)
}

// BitmapFactory ignores EXIF orientation, same fix as HiddenMediaRepository
private fun exifRotationDegrees(exif: ExifInterface?): Int =
    when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }

private fun rotateBitmapIfNeeded(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun calculateInSampleSize(options: BitmapFactory.Options, maxDimension: Int): Int {
    if (options.outWidth <= 0 || options.outHeight <= 0) return 1
    // Longest edge at or below maxDimension, so a 12MP photo previews at about 2000 px, not full size.
    val longest = maxOf(options.outWidth, options.outHeight)
    var inSampleSize = 1
    while (longest / inSampleSize > maxDimension) inSampleSize *= 2
    return inSampleSize
}

private fun decodePreviewBitmapPlatform(context: Context, uri: Uri, maxDimension: Int): Bitmap? =
    try {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOptions)
        }
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            null
        } else {
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(boundsOptions, maxDimension)
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
            val rotation = exifRotationDegrees(readExif { context.contentResolver.openInputStream(uri) })
            decoded?.let { rotateBitmapIfNeeded(it, rotation) }
        }
    } catch (e: Exception) {
        null
    }

private fun decodePreviewBitmapFromFilePlatform(file: File, maxDimension: Int): Bitmap? =
    try {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            null
        } else {
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(boundsOptions, maxDimension)
            }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            val rotation = exifRotationDegrees(readExif(file))
            decoded?.let { rotateBitmapIfNeeded(it, rotation) }
        }
    } catch (e: Exception) {
        null
    }

// Safety cap only, not a downsampling policy: huge images crash Canvas
private fun imageSizeOf(file: File): Pair<Int, Int>? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    val rotation = exifRotationDegrees(readExif(file))
    return if (rotation == 90 || rotation == 270) options.outHeight to options.outWidth else options.outWidth to options.outHeight
}

// The 56dp filter previews only need a small bitmap, not the full preview.
private fun filterThumbnailOf(source: Bitmap): ImageBitmap {
    val scale = 192f / maxOf(source.width, source.height)
    val small = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true
        )
    } else {
        source
    }
    return small.asImageBitmap()
}

// BitmapFactory cannot read HEIC from a stream (it decoded to null, which showed the "can't be edited" screen), so
// when it fails the platform ImageDecoder is used. It applies the photo's rotation itself, so none is added after.
private fun decodeWithImageDecoder(source: ImageDecoder.Source, maxDimension: Int?): Bitmap? = try {
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val width = info.size.width
        val height = info.size.height
        val longest = maxOf(width, height)
        val scale = when {
            maxDimension != null && longest > maxDimension -> maxDimension.toFloat() / longest
            maxDimension == null && width.toLong() * height > MAX_SAFE_DECODE_PIXELS ->
                kotlin.math.sqrt(MAX_SAFE_DECODE_PIXELS.toDouble() / (width.toDouble() * height)).toFloat()
            else -> 1f
        }
        if (scale < 1f) {
            decoder.setTargetSize((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
        }
    }
} catch (e: Exception) {
    null
} catch (e: OutOfMemoryError) {
    null
}

private fun decodePreviewBitmap(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? =
    decodePreviewBitmapPlatform(context, uri, maxDimension)
        ?: decodeWithImageDecoder(ImageDecoder.createSource(context.contentResolver, uri), maxDimension)

private fun decodePreviewBitmapFromFile(file: File, maxDimension: Int = 2048): Bitmap? =
    decodePreviewBitmapFromFilePlatform(file, maxDimension)
        ?: decodeWithImageDecoder(ImageDecoder.createSource(file), maxDimension)

private fun decodeFullResolutionBitmap(context: Context, uri: Uri): Bitmap? =
    decodeFullResolutionBitmapPlatform(context, uri)
        ?: decodeWithImageDecoder(ImageDecoder.createSource(context.contentResolver, uri), null)

private fun decodeFullResolutionBitmapFromFile(file: File): Bitmap? =
    decodeFullResolutionBitmapFromFilePlatform(file)
        ?: decodeWithImageDecoder(ImageDecoder.createSource(file), null)

private const val MAX_SAFE_DECODE_PIXELS = 40_000_000L // ~40MP

private fun calculateSafetyInSampleSize(options: BitmapFactory.Options): Int {
    val totalPixels = options.outWidth.toLong() * options.outHeight.toLong()
    if (totalPixels <= MAX_SAFE_DECODE_PIXELS) return 1
    var inSampleSize = 1
    while ((options.outWidth / (inSampleSize * 2)).toLong() * (options.outHeight / (inSampleSize * 2)).toLong() > MAX_SAFE_DECODE_PIXELS) {
        inSampleSize *= 2
    }
    return inSampleSize
}

private fun decodeFullResolutionBitmapPlatform(context: Context, uri: Uri): Bitmap? =
    try {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOptions)
        }
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateSafetyInSampleSize(boundsOptions)
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        }
        val rotation = exifRotationDegrees(readExif { context.contentResolver.openInputStream(uri) })
        decoded?.let { rotateBitmapIfNeeded(it, rotation) }
    } catch (e: Exception) {
        null
    }

private fun decodeFullResolutionBitmapFromFilePlatform(file: File): Bitmap? =
    try {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateSafetyInSampleSize(boundsOptions)
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        val rotation = exifRotationDegrees(readExif(file))
        decoded?.let { rotateBitmapIfNeeded(it, rotation) }
    } catch (e: Exception) {
        null
    }

// Rotate is a 90 degree multiple, straighten is the fine slider. Kept apart: other angles enlarge the bounding box.
private fun applyGeometricTransform(
    source: Bitmap,
    rotationDegrees: Float,
    straightenDegrees: Float,
    flipH: Boolean,
    flipV: Boolean
): Bitmap {
    if (straightenDegrees == 0f) {
        val matrix = Matrix().apply {
            postRotate(rotationDegrees)
            postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f)
        }
        val bounds = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        matrix.mapRect(bounds)
        matrix.postTranslate(-bounds.left, -bounds.top)
        val outWidth = bounds.width().toInt().coerceAtLeast(1)
        val outHeight = bounds.height().toInt().coerceAtLeast(1)
        val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        return output
    }

    // Straighten: keep the frame size, scale the content up to cover it (no transparent corners)
    val swapped = ((rotationDegrees % 180f) + 180f) % 180f == 90f
    val outWidth = if (swapped) source.height else source.width
    val outHeight = if (swapped) source.width else source.height
    val theta = Math.toRadians(straightenDegrees.toDouble())
    val fillScale = (
        kotlin.math.abs(kotlin.math.cos(theta)) +
            kotlin.math.abs(kotlin.math.sin(theta)) * (outWidth.toDouble() / outHeight.toDouble())
        ).toFloat()

    val matrix = Matrix().apply {
        // Transform around the source center
        postTranslate(-source.width / 2f, -source.height / 2f)
        postRotate(rotationDegrees + straightenDegrees)
        postScale((if (flipH) -1f else 1f) * fillScale, (if (flipV) -1f else 1f) * fillScale)
        postTranslate(outWidth / 2f, outHeight / 2f)
    }
    val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(source, matrix, paint)
    return output
}

// Perspective via a 4-corner projective warp, an affine transform can't make a trapezoid
private fun applyPerspective(source: Bitmap, horizontal: Float, vertical: Float): Bitmap {
    if (horizontal == 0f && vertical == 0f) return source
    val w = source.width.toFloat()
    val h = source.height.toFloat()

    val vInset = vertical.coerceIn(-1f, 1f) * h * 0.35f
    val hInset = horizontal.coerceIn(-1f, 1f) * w * 0.35f
    val topInset = if (vInset > 0f) vInset else 0f
    val bottomInset = if (vInset < 0f) -vInset else 0f
    val rightInset = if (hInset > 0f) hInset else 0f
    val leftInset = if (hInset < 0f) -hInset else 0f

    val src = floatArrayOf(
        0f + leftInset, 0f + topInset,
        w - rightInset, 0f + topInset,
        w - rightInset, h - bottomInset,
        0f + leftInset, h - bottomInset
    )
    val dst = floatArrayOf(
        0f, 0f,
        w, 0f,
        w, h,
        0f, h
    )

    val matrix = Matrix()
    matrix.setPolyToPoly(src, 0, dst, 0, 4)
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(source, matrix, paint)
    return output
}

private fun applyColorMatrix(source: Bitmap, colorMatrix: FloatArray): Bitmap {
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(colorMatrix)
    }
    canvas.drawBitmap(source, 0f, 0f, paint)
    return output
}

private fun cropBitmap(source: Bitmap, cropRect: CropRect): Bitmap {
    if (cropRect.left <= 0f && cropRect.top <= 0f && cropRect.right >= 1f && cropRect.bottom >= 1f) return source
    val x = (cropRect.left * source.width).toInt().coerceIn(0, source.width - 1)
    val y = (cropRect.top * source.height).toInt().coerceIn(0, source.height - 1)
    val w = (cropRect.width * source.width).toInt().coerceIn(1, source.width - x)
    val h = (cropRect.height * source.height).toInt().coerceIn(1, source.height - y)
    return Bitmap.createBitmap(source, x, y, w, h)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    target: EditTarget,
    darkTheme: Boolean,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    // A deleted edit leaves nothing to show, so the caller can leave the viewer too instead of landing on it.
    onDeleted: () -> Unit = onSaved,
    onHiddenSizeChanged: (width: Int, height: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    // Same colors as the viewers: black only in dark mode, the app background in light mode.
    val screenBackground = if (darkTheme) Color.Black else MaterialTheme.colorScheme.background
    val chromeColor = if (darkTheme) Color.White else Color.Black
    val pillBackground = if (darkTheme) GlassPill.fixedGlassBackground else GlassPill.adaptiveGlassBackground
    val pillBorder = if (darkTheme) null else GlassPill.adaptiveBorder
    val popupBackground = if (darkTheme) GlassPill.fixedPopupBackground else GlassPill.adaptivePopupBackground
    val coroutineScope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var autoEnhanceTargets by remember { mutableStateOf<AutoEnhanceTargets?>(null) }
    var editorMode by remember { mutableStateOf(EditorMode.STYLES) }
    var rotationDegrees by remember { mutableStateOf(0f) }
    var straightenDegrees by remember { mutableStateOf(0f) }
    var perspectiveH by remember { mutableStateOf(0f) }
    var perspectiveV by remember { mutableStateOf(0f) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(PhotoFilter.NONE) }
    var adjustments by remember { mutableStateOf(Adjustments()) }
    var selectedAdjustmentType by remember { mutableStateOf(AdjustmentType.BRIGHTNESS) }
    var cropRect by remember { mutableStateOf(CropRect()) }
    var selectedAspectPreset by remember { mutableStateOf(AspectPreset.entries.first()) }
    var isSaving by remember { mutableStateOf(false) }
    var pendingTransformedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // Real on-screen pixel size, needed for the sharpen shader (RenderEffect works in layer pixels)
    var imageSizePx by remember { mutableStateOf(IntSize.Zero) }
    // Pinch zoom to inspect detail, preview only, reuses ZoomableImage's gesture detector
    var userZoomScale by remember { mutableStateOf(1f) }
    var userZoomOffset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(editorMode) {
        if (editorMode == EditorMode.CROP) {
            userZoomScale = 1f
            userZoomOffset = Offset.Zero
        }
    }
    fun clampUserZoomOffset(newScale: Float, candidate: Offset): Offset {
        val maxX = (imageSizePx.width * (newScale - 1f) / 2f).coerceAtLeast(0f)
        val maxY = (imageSizePx.height * (newScale - 1f) / 2f).coerceAtLeast(0f)
        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    // Original is kept aside on the first saved edit, for Revert
    val originals = remember { OriginalsStore(context) }
    val originalKey = remember(target) {
        when (target) {
            is EditTarget.MediaStoreTarget -> OriginalsStore.mediaKey(target.item.id)
            is EditTarget.HiddenFileTarget -> OriginalsStore.hiddenKey(target.file.name)
        }
    }
    var hasOriginal by remember(originalKey) { mutableStateOf(originals.has(originalKey)) }
    // An edited copy can be deleted from here, that is Revert for library photos
    var isEditedCopy by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        (target as? EditTarget.MediaStoreTarget)?.let { isEditedCopy = LocalMetadataStore(context).isEditedCopy(it.item.id) }
    }
    var showRevertConfirm by remember { mutableStateOf(false) }
    var isReverting by remember { mutableStateOf(false) }

    val hasUnsavedChanges = rotationDegrees != 0f || straightenDegrees != 0f ||
        perspectiveH != 0f || perspectiveV != 0f || flipHorizontal || flipVertical ||
        selectedFilter != PhotoFilter.NONE || !adjustments.isNeutral ||
        !(cropRect.left <= 0f && cropRect.top <= 0f && cropRect.right >= 1f && cropRect.bottom >= 1f)

    fun requestBack() {
        if (hasUnsavedChanges) showDiscardConfirm = true else onBack()
    }

    BackHandler(enabled = hasUnsavedChanges) {
        showDiscardConfirm = true
    }

    val writeRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val bitmap = pendingTransformedBitmap
        val mediaTarget = target as? EditTarget.MediaStoreTarget
        if (result.resultCode == Activity.RESULT_OK && bitmap != null && mediaTarget != null) {
            coroutineScope.launch {
                val success = withContext(Dispatchers.IO) {
                    try {
                        // Keep the original BEFORE the write truncates the file. If that fails, don't overwrite.
                        val originalKept = originals.stashIfAbsent(originalKey) {
                            context.contentResolver.openInputStream(mediaTarget.item.uri)
                        }
                        if (!originalKept) return@withContext false
                        context.contentResolver.openOutputStream(mediaTarget.item.uri, "rwt")?.use { output ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
                        } ?: false
                    } catch (e: Exception) {
                        false
                    }
                }
                isSaving = false
                pendingTransformedBitmap = null
                if (success) {
                    hasOriginal = true
                    val uriKey = mediaTarget.item.uri.toString()
                    val loader = context.imageLoader
                    loader.memoryCache?.remove(MemoryCache.Key(uriKey))
                    loader.diskCache?.remove(uriKey)
                    onSaved()
                }
            }
        } else {
            isSaving = false
            pendingTransformedBitmap = null
        }
    }

    fun clearImageCaches(keys: List<String>) {
        val loader = context.imageLoader
        for (key in keys) {
            loader.memoryCache?.remove(MemoryCache.Key(key))
            loader.diskCache?.remove(key)
        }
    }

    // Library Revert: same consent as saving, writes the stored original back, then drops it
    val revertWriteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val mediaTarget = target as? EditTarget.MediaStoreTarget
        if (result.resultCode == Activity.RESULT_OK && mediaTarget != null) {
            coroutineScope.launch {
                val success = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(mediaTarget.item.uri, "rwt")?.use { output ->
                            originals.copyTo(originalKey, output)
                        } ?: false
                    } catch (e: Exception) {
                        false
                    }
                }
                isReverting = false
                if (success) {
                    originals.delete(originalKey)
                    hasOriginal = false
                    clearImageCaches(listOf(mediaTarget.item.uri.toString()))
                    onSaved()
                }
            }
        } else {
            isReverting = false
        }
    }

    fun onRevertConfirmed() {
        isReverting = true
        when (target) {
            is EditTarget.MediaStoreTarget -> if (isEditedCopy) {
                coroutineScope.launch {
                    val deleted = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.delete(target.item.uri, null, null) > 0
                        } catch (e: SecurityException) {
                            false
                        }
                    }
                    isReverting = false
                    if (deleted) {
                        LocalMetadataStore(context).removeEditedCopy(target.item.id)
                        clearImageCaches(listOf(target.item.uri.toString()))
                        onDeleted()
                    } else {
                        Toast.makeText(context, "Could not delete the edited copy.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                val pendingIntent = MediaStore.createWriteRequest(
                    context.contentResolver,
                    listOf(target.item.uri)
                )
                revertWriteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            }
            is EditTarget.HiddenFileTarget -> {
                coroutineScope.launch {
                    val success = withContext(Dispatchers.IO) {
                        try {
                            val restored = target.file.outputStream().use { out -> originals.copyTo(originalKey, out) }
                            restored && rewriteHiddenThumbnail(target.file, target.thumbnailFile)
                        } catch (e: Exception) {
                            false
                        }
                    }
                    isReverting = false
                    if (success) {
                        originals.delete(originalKey)
                        hasOriginal = false
                        clearImageCaches(listOf(target.file.absolutePath, target.thumbnailFile.absolutePath))
                        withContext(Dispatchers.IO) { imageSizeOf(target.file) }?.let { onHiddenSizeChanged(it.first, it.second) }
                        onSaved()
                    }
                }
            }
        }
    }

    LaunchedEffect(target) {
        loadFailed = false
        autoEnhanceTargets = null
        val bitmap = withContext(Dispatchers.IO) {
            when (target) {
                is EditTarget.MediaStoreTarget -> decodePreviewBitmap(context, target.item.uri)
                is EditTarget.HiddenFileTarget -> decodePreviewBitmapFromFile(target.file)
            }
        }
        sourceBitmap = bitmap
        loadFailed = bitmap == null
        if (bitmap != null) {
            // Chained after the decode, computeAutoEnhanceTargets downsamples itself
            autoEnhanceTargets = withContext(Dispatchers.Default) { computeAutoEnhanceTargets(bitmap) }
        }
    }

    fun onSaveClicked() {
        isSaving = true
        coroutineScope.launch {
            val transformed = withContext(Dispatchers.Default) {
                try {
                    val fullRes = when (target) {
                        is EditTarget.MediaStoreTarget -> decodeFullResolutionBitmap(context, target.item.uri)
                        is EditTarget.HiddenFileTarget -> decodeFullResolutionBitmapFromFile(target.file)
                    } ?: return@withContext null
                    // Each stage returns a new bitmap or its input, so the replaced input is freed (low peak memory)
                    var current: Bitmap = fullRes
                    current = applyPerspective(current, perspectiveH, perspectiveV).also { if (it !== current) current.recycle() }
                    current = applyGeometricTransform(current, rotationDegrees, straightenDegrees, flipHorizontal, flipVertical)
                        .also { if (it !== current) current.recycle() }
                    current = cropBitmap(current, cropRect).also { if (it !== current) current.recycle() }
                    val combinedMatrix = combinedColorMatrix(selectedFilter.matrix, adjustments)
                    current = applyColorMatrix(current, combinedMatrix).also { if (it !== current) current.recycle() }
                    current = applyShaderAdjustments(current, adjustments).also { if (it !== current) current.recycle() }
                    applyVignette(current, adjustments.vignette).also { if (it !== current) current.recycle() }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                } catch (e: OutOfMemoryError) {
                    null
                }
            }
            if (transformed == null) {
                isSaving = false
                return@launch
            }

            when (target) {
                is EditTarget.MediaStoreTarget -> {
                    // A new photo, never an overwrite: replacing a photo from another app asks for permission every time.
                    // Always saved as a JPEG named IMG_number.
                    val copyUri = MediaStoreRepository(context).saveEditedCopy(transformed)
                    isSaving = false
                    if (copyUri != null) {
                        // Sorted just before the original (+1 second) so the copy sits beside it
                        LocalMetadataStore(context).setSortDate(ContentUris.parseId(copyUri), target.item.dateAdded + 1)
                        LocalMetadataStore(context).addEditedCopy(ContentUris.parseId(copyUri))
                        Toast.makeText(context, "Saved as a new photo. The original is unchanged.", Toast.LENGTH_LONG).show()
                        onSaved()
                    } else {
                        Toast.makeText(context, "Could not save the edit.", Toast.LENGTH_LONG).show()
                    }
                }
                is EditTarget.HiddenFileTarget -> {
                    val success = withContext(Dispatchers.IO) {
                        try {
                            if (!originals.stashIfAbsent(originalKey) { target.file.inputStream() }) {
                                return@withContext false
                            }
                            target.file.outputStream().use { output ->
                                transformed.compress(Bitmap.CompressFormat.JPEG, 95, output)
                            } && run {
                                // Regenerate the grid thumbnail from the processed bitmap, not by re-decoding the file
                                val longerSide = maxOf(transformed.width, transformed.height)
                                val scale = 512f / longerSide
                                val thumbBitmap = if (scale < 1f) {
                                    Bitmap.createScaledBitmap(
                                        transformed,
                                        (transformed.width * scale).toInt().coerceAtLeast(1),
                                        (transformed.height * scale).toInt().coerceAtLeast(1),
                                        true
                                    )
                                } else {
                                    transformed
                                }
                                val thumbSaved = target.thumbnailFile.outputStream().use { output ->
                                    thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
                                }
                                if (thumbBitmap !== transformed) thumbBitmap.recycle()
                                thumbSaved
                            }
                        } catch (e: Exception) {
                            false
                        }
                    }
                    isSaving = false
                    if (success) {
                        val loader = context.imageLoader
                        for (path in listOf(target.file.absolutePath, target.thumbnailFile.absolutePath)) {
                            loader.memoryCache?.remove(MemoryCache.Key(path))
                            loader.diskCache?.remove(path)
                        }
                        onHiddenSizeChanged(transformed.width, transformed.height)
                        onSaved()
                    }
                }
            }
        }
    }

    fun adjustmentValue(type: AdjustmentType): Float = when (type) {
        AdjustmentType.AUTO_ENHANCE -> adjustments.autoEnhance
        AdjustmentType.EXPOSURE -> adjustments.exposure
        AdjustmentType.BRIGHTNESS -> adjustments.brightness
        AdjustmentType.HIGHLIGHTS -> adjustments.highlights
        AdjustmentType.SHADOWS -> adjustments.shadows
        AdjustmentType.CONTRAST -> adjustments.contrast
        AdjustmentType.BLACK_POINT -> adjustments.blackPoint
        AdjustmentType.SATURATION -> adjustments.saturation
        AdjustmentType.VIBRANCE -> adjustments.vibrance
        AdjustmentType.WARMTH -> adjustments.warmth
        AdjustmentType.TINT -> adjustments.tint
        AdjustmentType.SHARPNESS -> adjustments.sharpness
        AdjustmentType.DEFINITION -> adjustments.definition
        AdjustmentType.NOISE_REDUCTION -> adjustments.noiseReduction
        AdjustmentType.VIGNETTE -> adjustments.vignette
    }

    fun setAdjustmentValue(type: AdjustmentType, value: Float) {
        adjustments = when (type) {
            AdjustmentType.AUTO_ENHANCE -> adjustments.copy(autoEnhance = value)
            AdjustmentType.EXPOSURE -> adjustments.copy(exposure = value)
            AdjustmentType.BRIGHTNESS -> adjustments.copy(brightness = value)
            AdjustmentType.HIGHLIGHTS -> adjustments.copy(highlights = value)
            AdjustmentType.SHADOWS -> adjustments.copy(shadows = value)
            AdjustmentType.CONTRAST -> adjustments.copy(contrast = value)
            AdjustmentType.BLACK_POINT -> adjustments.copy(blackPoint = value)
            AdjustmentType.SATURATION -> adjustments.copy(saturation = value)
            AdjustmentType.VIBRANCE -> adjustments.copy(vibrance = value)
            AdjustmentType.WARMTH -> adjustments.copy(warmth = value)
            AdjustmentType.TINT -> adjustments.copy(tint = value)
            AdjustmentType.SHARPNESS -> adjustments.copy(sharpness = value)
            AdjustmentType.DEFINITION -> adjustments.copy(definition = value)
            AdjustmentType.NOISE_REDUCTION -> adjustments.copy(noiseReduction = value)
            AdjustmentType.VIGNETTE -> adjustments.copy(vignette = value)
        }
    }

    // Auto Enhance writes eight other fields at once, so it isn't routed through setAdjustmentValue
    fun applyAutoEnhance(strength: Float) {
        val targets = autoEnhanceTargets ?: return
        adjustments = adjustments.copy(
            autoEnhance = strength,
            exposure = targets.exposure * strength,
            contrast = targets.contrast * strength,
            blackPoint = targets.blackPoint * strength,
            highlights = targets.highlights * strength,
            shadows = targets.shadows * strength,
            vibrance = targets.vibrance * strength,
            warmth = targets.warmth * strength,
            tint = targets.tint * strength
        )
    }

    Scaffold(
        containerColor = screenBackground,
        topBar = {
            TopAppBar(
                title = { Text("Edit", color = chromeColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { requestBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel", tint = chromeColor)
                    }
                },
                actions = {
                    // Only for a photo with a stored original
                    if (hasOriginal || isEditedCopy) {
                        Box(
                            Modifier
                                .padding(end = 4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30))
                                .clickable(enabled = !isSaving && !isReverting) { showRevertConfirm = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(if (isEditedCopy) "Delete Edit" else "Revert", color = chromeColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    IconButton(
                        enabled = sourceBitmap != null && !isSaving && !isReverting,
                        onClick = { onSaveClicked() }
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Save", tint = chromeColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = screenBackground)
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .background(screenBackground)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 8.dp)
            ) {
                when (editorMode) {
                    EditorMode.STYLES -> {
                        val bitmap = sourceBitmap
                        if (bitmap != null) {
                            val filterThumbnail = remember(bitmap) { filterThumbnailOf(bitmap) }
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                                items(PhotoFilter.entries, key = { it.name }) { filter ->
                                    val selected = filter == selectedFilter
                                    Column(
                                        Modifier
                                            .padding(end = 12.dp)
                                            .clickable { selectedFilter = filter },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Image(
                                            bitmap = filterThumbnail,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            colorFilter = ColorFilter.colorMatrix(ComposeColorMatrix(filter.matrix)),
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .then(
                                                    if (selected) {
                                                        Modifier.border(2.dp, Color(0xFFFFD60A), RoundedCornerShape(10.dp))
                                                    } else Modifier
                                                )
                                        )
                                        Text(
                                            filter.label,
                                            color = if (selected) Color(0xFFFFD60A) else chromeColor.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    EditorMode.ADJUST -> {
                        // LazyRow: 9 categories don't fit evenly on a phone, it scrolls like iOS
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            items(AdjustmentType.entries, key = { it.name }) { type ->
                                val selected = type == selectedAdjustmentType
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(chromeColor.copy(alpha = 0.1f))
                                            .then(
                                                if (selected) {
                                                    Modifier.border(2.dp, Color(0xFFFFD60A), CircleShape)
                                                } else Modifier
                                            )
                                            .clickable {
                                                selectedAdjustmentType = type
                                                // A toggle: at 0 it jumps to 100% (the full computed correction), again resets all eight fields to 0
                                                if (type == AdjustmentType.AUTO_ENHANCE) {
                                                    applyAutoEnhance(if (adjustments.autoEnhance == 0f) 1f else 0f)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val value = adjustmentValue(type)
                                        if (value != 0f) {
                                            val counterValue = (value * 100).toInt()
                                            Text(
                                                text = if (counterValue > 0) "+$counterValue" else "$counterValue",
                                                color = chromeColor,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Icon(type.icon, contentDescription = type.label, tint = chromeColor)
                                        }
                                    }
                                    Text(
                                        type.label,
                                        color = if (selected) Color(0xFFFFD60A) else chromeColor.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                        // One slider at a time, for the selected category (like iOS)
                        Slider(
                            value = adjustmentValue(selectedAdjustmentType),
                            onValueChange = {
                                if (selectedAdjustmentType == AdjustmentType.AUTO_ENHANCE) {
                                    applyAutoEnhance(it)
                                } else {
                                    setAdjustmentValue(selectedAdjustmentType, it)
                                }
                            },
                            valueRange = if (
                                selectedAdjustmentType == AdjustmentType.AUTO_ENHANCE ||
                                selectedAdjustmentType == AdjustmentType.NOISE_REDUCTION
                            ) {
                                0f..1f
                            } else {
                                -1f..1f
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        )
                    }
                    EditorMode.CROP -> {
                        GlassPillContainer(modifier = Modifier.padding(vertical = 4.dp), backgroundColor = pillBackground, border = pillBorder) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val bitmap = sourceBitmap
                                AspectPreset.entries.forEach { preset ->
                                    val selected = preset == selectedAspectPreset
                                    Text(
                                        preset.label,
                                        color = if (selected) Color(0xFFFFD60A) else chromeColor,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier
                                            .clickable {
                                                selectedAspectPreset = preset
                                                if (bitmap != null) {
                                                    val swapped = rotationDegrees % 180f != 0f
                                                    val imageAspect = if (swapped) {
                                                        bitmap.height.toFloat() / bitmap.width.toFloat()
                                                    } else {
                                                        bitmap.width.toFloat() / bitmap.height.toFloat()
                                                    }
                                                    cropRect = if (preset.ratio == null) {
                                                        CropRect()
                                                    } else {
                                                        cropRectForAspect(preset.ratio, imageAspect)
                                                    }
                                                }
                                            }
                                    )
                                }
                            }
                        }
                        GeometrySliderRow(
                            chromeColor = chromeColor,
                            label = "Straighten",
                            value = straightenDegrees,
                            valueRange = -45f..45f,
                            displayValue = (if (straightenDegrees > 0f) "+" else "") + "%.1f".format(straightenDegrees) + "°",
                            onValueChange = { straightenDegrees = it }
                        )
                        GeometrySliderRow(
                            chromeColor = chromeColor,
                            label = "Vertical",
                            value = perspectiveV,
                            valueRange = -1f..1f,
                            displayValue = (perspectiveV * 100).toInt().let { if (it > 0) "+$it" else "$it" },
                            onValueChange = { perspectiveV = it }
                        )
                        GeometrySliderRow(
                            chromeColor = chromeColor,
                            label = "Horizontal",
                            value = perspectiveH,
                            valueRange = -1f..1f,
                            displayValue = (perspectiveH * 100).toInt().let { if (it > 0) "+$it" else "$it" },
                            onValueChange = { perspectiveH = it }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(onClick = {
                                rotationDegrees = (rotationDegrees + 90f) % 360f
                                cropRect = CropRect()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate", tint = chromeColor)
                            }
                            IconButton(onClick = { flipHorizontal = !flipHorizontal }) {
                                Icon(Icons.Filled.Flip, contentDescription = "Flip horizontal", tint = chromeColor)
                            }
                            IconButton(onClick = { flipVertical = !flipVertical }) {
                                Icon(
                                    imageVector = Icons.Filled.Flip,
                                    contentDescription = "Flip vertical",
                                    tint = chromeColor,
                                    modifier = Modifier.graphicsLayer { rotationZ = 90f }
                                )
                            }
                        }
                    }
                }

                BottomModeBar(editorMode, chromeColor, pillBackground, pillBorder) { editorMode = it }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = sourceBitmap
            when {
                bitmap != null -> {
                    // Same aspect-matched box in all three modes so the image doesn't move
                    val combinedMatrix = combinedColorMatrix(selectedFilter.matrix, adjustments)
                    val previewRenderEffect = remember(adjustments, imageSizePx) {
                        buildPreviewRenderEffect(adjustments, imageSizePx.width, imageSizePx.height)
                    }
                    val swapped = rotationDegrees % 180f != 0f
                    val imageAspect = if (swapped) {
                        bitmap.height.toFloat() / bitmap.width.toFloat()
                    } else {
                        bitmap.width.toFloat() / bitmap.height.toFloat()
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(imageAspect)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.colorMatrix(ComposeColorMatrix(combinedMatrix)),
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { imageSizePx = it }
                                .then(
                                    if (editorMode != EditorMode.CROP) {
                                        Modifier.pointerInput(editorMode) {
                                            detectPinchPanGestures(
                                                getScale = { userZoomScale },
                                                onGesture = { pan, zoom ->
                                                    val newScale = (userZoomScale * zoom).coerceIn(1f, 6f)
                                                    userZoomScale = newScale
                                                    userZoomOffset = if (newScale <= 1f) {
                                                        Offset.Zero
                                                    } else {
                                                        clampUserZoomOffset(newScale, userZoomOffset + pan)
                                                    }
                                                }
                                            )
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                                .graphicsLayer {
                                    rotationZ = rotationDegrees + straightenDegrees
                                    // Straighten auto-zoom preview, same fillScale math as applyGeometricTransform
                                    val fillScale = if (straightenDegrees != 0f) {
                                        val theta = Math.toRadians(straightenDegrees.toDouble())
                                        (
                                            kotlin.math.abs(kotlin.math.cos(theta)) +
                                                kotlin.math.abs(kotlin.math.sin(theta)) * (size.width / size.height)
                                            ).toFloat()
                                    } else {
                                        1f
                                    }
                                    scaleX = (if (flipHorizontal) -1f else 1f) * fillScale * userZoomScale
                                    scaleY = (if (flipVertical) -1f else 1f) * fillScale * userZoomScale
                                    translationX = userZoomOffset.x
                                    translationY = userZoomOffset.y
                                    // Perspective preview is a 3D tilt (rotationX/Y), graphicsLayer has no projective transform.
                                    // Looks like the setPolyToPoly warp used on save.
                                    if (perspectiveV != 0f || perspectiveH != 0f) {
                                        cameraDistance = 12f * density
                                        rotationX = -perspectiveV * 15f
                                        rotationY = perspectiveH * 15f
                                    }
                                    // Highlights, Shadows, Vibrance, Sharpness are AGSL shaders, not part of combinedMatrix. Null when none are active.
                                    renderEffect = previewRenderEffect
                                }
                        )
                        // Live vignette is a radial gradient overlay, a bitmap redraw per frame would make dragging choppy
                        if (adjustments.vignette != 0f) {
                            val strength = adjustments.vignette
                            val darken = strength > 0f
                            val amount = kotlin.math.abs(strength).coerceIn(0f, 1f)
                            ComposeCanvas(Modifier.fillMaxSize()) {
                                val edgeColor = if (darken) {
                                    Color.Black.copy(alpha = amount * 0.7f)
                                } else {
                                    Color.White.copy(alpha = amount * 0.5f)
                                }
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color.Transparent, edgeColor),
                                        center = center,
                                        radius = size.maxDimension * 0.75f
                                    ),
                                    blendMode = if (darken) BlendMode.Multiply else BlendMode.Screen
                                )
                            }
                        }
                        if (editorMode == EditorMode.CROP) {
                            CropOverlay(
                                cropRect = cropRect,
                                onCropRectChange = { cropRect = it }
                            )
                        }
                    }
                }
                loadFailed -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val isHiddenVideo = target is EditTarget.HiddenFileTarget && target.isVideo
                        Text(if (isHiddenVideo) "Videos in Hidden can't be edited" else "This photo can't be edited", color = chromeColor)
                        Text(
                            if (isHiddenVideo) {
                                "Move the video out of Hidden first, then trim it from the library."
                            } else {
                                "The format isn't supported (e.g. RAW formats aren't decodable by the platform's bitmap tools)"
                            },
                            color = chromeColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp)
                        )
                    }
                }
                else -> CircularProgressIndicator(color = chromeColor)
            }

            if (isSaving) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }

    if (showRevertConfirm) {
        AlertDialog(
            onDismissRequest = { showRevertConfirm = false },
            shape = GlassPill.shape,
            containerColor = popupBackground,
            titleContentColor = chromeColor,
            textContentColor = chromeColor,
            title = { Text(if (isEditedCopy) "Delete this edit?" else "Revert to original?") },
            text = {
                Text(
                    if (isEditedCopy) "This deletes the edited copy. The photo it was made from is unchanged."
                    else "This restores the photo to how it was before it was edited. The saved edits can't be recovered."
                )
            },
            confirmButton = {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Red.copy(alpha = 0.15f))
                        .clickable {
                            showRevertConfirm = false
                            onRevertConfirmed()
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(if (isEditedCopy) "Delete Edit" else "Revert to Original", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevertConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            shape = GlassPill.shape,
            containerColor = popupBackground,
            titleContentColor = chromeColor,
            textContentColor = chromeColor,
            title = { Text("Discard changes?") },
            text = { Text("Are you sure you want to discard the changes?") },
            confirmButton = {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Red.copy(alpha = 0.15f))
                        .clickable {
                            showDiscardConfirm = false
                            onBack()
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Discard Changes", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GeometrySliderRow(
    chromeColor: Color,
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = chromeColor, fontSize = 13.sp)
                if (value != 0f) {
                    IconButton(
                        onClick = { onValueChange(0f) },
                        modifier = Modifier.size(28.dp).padding(start = 4.dp)
                    ) {
                        Icon(
                            Icons.Filled.RestartAlt,
                            contentDescription = "Reset $label",
                            tint = chromeColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Text(displayValue, color = chromeColor.copy(alpha = 0.7f), fontSize = 13.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun BottomModeBar(
    mode: EditorMode,
    chromeColor: Color,
    pillBackground: Color,
    pillBorder: BorderStroke?,
    onModeChange: (EditorMode) -> Unit
) {
    GlassPillContainer(backgroundColor = pillBackground, border = pillBorder) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                Triple(EditorMode.STYLES, Icons.Filled.AutoAwesome, "Styles"),
                Triple(EditorMode.ADJUST, Icons.Filled.Tune, "Adjust"),
                Triple(EditorMode.CROP, Icons.Filled.Crop, "Crop")
            ).forEach { (m, icon, label) ->
                val selected = m == mode
                val tint = if (selected) Color(0xFFFFD60A) else chromeColor
                Column(
                    Modifier.clickable { onModeChange(m) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(icon, contentDescription = label, tint = tint)
                    Text(label, color = tint, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

// Rewrites the hidden grid thumbnail from the full file: after a revert the grid would show the edited one
private fun rewriteHiddenThumbnail(file: File, thumbnailFile: File): Boolean {
    val bitmap = decodePreviewBitmapFromFile(file) ?: return false
    return try {
        val longerSide = maxOf(bitmap.width, bitmap.height)
        val scale = 512f / longerSide
        val thumb = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        val saved = thumbnailFile.outputStream().use { output ->
            thumb.compress(Bitmap.CompressFormat.JPEG, 85, output)
        }
        if (thumb !== bitmap) thumb.recycle()
        saved
    } finally {
        bitmap.recycle()
    }
}
