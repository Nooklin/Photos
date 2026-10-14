package org.nooklin.photos.ui.editor
import androidx.annotation.StringRes
import org.nooklin.photos.R
import org.nooklin.photos.appString

// Fractional (0..1) coordinates, so container size changes don't matter
data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

enum class AspectPreset(@StringRes val labelRes: Int, val ratio: Float?) {
    FREE(R.string.crop_free, null),
    SQUARE(R.string.crop_ratio_1_1, 1f),
    FOUR_THREE(R.string.crop_ratio_4_3, 4f / 3f),
    SIXTEEN_NINE(R.string.crop_ratio_16_9, 16f / 9f);
    val label: String get() = appString(labelRes)
}

fun cropRectForAspect(ratio: Float, imageAspect: Float): CropRect {
    return if (ratio > imageAspect) {
        // Wider target: full width, centered vertically
        val heightFraction = imageAspect / ratio
        val top = (1f - heightFraction) / 2f
        CropRect(0f, top, 1f, top + heightFraction)
    } else {
        // Taller target: full height, centered horizontally
        val widthFraction = ratio / imageAspect
        val left = (1f - widthFraction) / 2f
        CropRect(left, 0f, left + widthFraction, 1f)
    }
}
