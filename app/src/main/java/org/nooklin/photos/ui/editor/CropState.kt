package org.nooklin.photos.ui.editor

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

enum class AspectPreset(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    FOUR_THREE("4:3", 4f / 3f),
    SIXTEEN_NINE("16:9", 16f / 9f)
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
