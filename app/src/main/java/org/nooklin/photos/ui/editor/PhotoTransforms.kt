package org.nooklin.photos.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

// The 56dp filter previews only need a small bitmap, not the full preview.
internal fun filterThumbnailOf(source: Bitmap): ImageBitmap {
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

internal fun applyGeometricTransform(
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
internal fun applyPerspective(source: Bitmap, horizontal: Float, vertical: Float): Bitmap {
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

internal fun applyColorMatrix(source: Bitmap, colorMatrix: FloatArray): Bitmap {
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(colorMatrix)
    }
    canvas.drawBitmap(source, 0f, 0f, paint)
    return output
}

internal fun cropBitmap(source: Bitmap, cropRect: CropRect): Bitmap {
    if (cropRect.left <= 0f && cropRect.top <= 0f && cropRect.right >= 1f && cropRect.bottom >= 1f) return source
    val x = (cropRect.left * source.width).toInt().coerceIn(0, source.width - 1)
    val y = (cropRect.top * source.height).toInt().coerceIn(0, source.height - 1)
    val w = (cropRect.width * source.width).toInt().coerceIn(1, source.width - x)
    val h = (cropRect.height * source.height).toInt().coerceIn(1, source.height - y)
    return Bitmap.createBitmap(source, x, y, w, h)
}

// Rewrites the hidden grid thumbnail from the full file: after a revert the grid would show the edited one
internal fun rewriteHiddenThumbnail(file: File, thumbnailFile: File): Boolean {
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

// the format a hidden file is written back in: PNG for .png, lossless WebP for .webp, else JPEG
internal fun compressFormatFor(extension: String): Bitmap.CompressFormat = when (extension.lowercase()) {
    "png" -> Bitmap.CompressFormat.PNG
    "webp" -> Bitmap.CompressFormat.WEBP_LOSSLESS
    else -> Bitmap.CompressFormat.JPEG
}
