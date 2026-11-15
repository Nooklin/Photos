package org.nooklin.photos.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.layout.size
import org.nooklin.photos.data.readExif
import java.io.File
import android.graphics.ImageDecoder

// BitmapFactory ignores EXIF orientation, same fix as HiddenMediaRepository
internal fun exifRotationDegrees(exif: ExifInterface?): Int =
    when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }

internal fun rotateBitmapIfNeeded(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

internal fun calculateInSampleSize(options: BitmapFactory.Options, maxDimension: Int): Int {
    if (options.outWidth <= 0 || options.outHeight <= 0) return 1
    // Longest edge at or below maxDimension, so a 12MP photo previews at about 2000 px, not full size.
    val longest = maxOf(options.outWidth, options.outHeight)
    var inSampleSize = 1
    while (longest / inSampleSize > maxDimension) inSampleSize *= 2
    return inSampleSize
}

internal fun decodePreviewBitmapPlatform(context: Context, uri: Uri, maxDimension: Int): Bitmap? =
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

internal fun decodePreviewBitmapFromFilePlatform(file: File, maxDimension: Int): Bitmap? =
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
internal fun imageSizeOf(file: File): Pair<Int, Int>? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    val rotation = exifRotationDegrees(readExif(file))
    return if (rotation == 90 || rotation == 270) options.outHeight to options.outWidth else options.outWidth to options.outHeight
}

// BitmapFactory cannot read HEIC from a stream (it decoded to null, which showed the "can't be edited" screen)
internal fun decodeWithImageDecoder(source: ImageDecoder.Source, maxDimension: Int?): Bitmap? = try {
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

internal fun decodePreviewBitmap(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? =
    decodePreviewBitmapPlatform(context, uri, maxDimension)
        ?: decodeWithImageDecoder(ImageDecoder.createSource(context.contentResolver, uri), maxDimension)

internal fun decodePreviewBitmapFromFile(file: File, maxDimension: Int = 2048): Bitmap? =
    decodePreviewBitmapFromFilePlatform(file, maxDimension)
        ?: decodeWithImageDecoder(ImageDecoder.createSource(file), maxDimension)

internal fun decodeFullResolutionBitmap(context: Context, uri: Uri): Bitmap? =
    decodeFullResolutionBitmapPlatform(context, uri)
        ?: decodeWithImageDecoder(ImageDecoder.createSource(context.contentResolver, uri), null)

internal fun decodeFullResolutionBitmapFromFile(file: File): Bitmap? =
    decodeFullResolutionBitmapFromFilePlatform(file)
        ?: decodeWithImageDecoder(ImageDecoder.createSource(file), null)

internal const val MAX_SAFE_DECODE_PIXELS = 40_000_000L // ~40MP

internal fun calculateSafetyInSampleSize(options: BitmapFactory.Options): Int {
    val totalPixels = options.outWidth.toLong() * options.outHeight.toLong()
    if (totalPixels <= MAX_SAFE_DECODE_PIXELS) return 1
    var inSampleSize = 1
    while ((options.outWidth / (inSampleSize * 2)).toLong() * (options.outHeight / (inSampleSize * 2)).toLong() > MAX_SAFE_DECODE_PIXELS) {
        inSampleSize *= 2
    }
    return inSampleSize
}

internal fun decodeFullResolutionBitmapPlatform(context: Context, uri: Uri): Bitmap? =
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

internal fun decodeFullResolutionBitmapFromFilePlatform(file: File): Bitmap? =
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
