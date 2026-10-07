package org.nooklin.photos.ui.common

import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation

class FaceCropTransformation(
    private val xMin: Float,
    private val yMin: Float,
    private val xMax: Float,
    private val yMax: Float
) : Transformation() {
    // Distinct per box, or two faces from one photo collide in the cache
    override val cacheKey: String = "face_crop($xMin,$yMin,$xMax,$yMax)"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        // The grid's own decode returns a hardware bitmap, whose pixels cannot be cropped on the CPU (the crop came out black).
        if (input.config == Bitmap.Config.HARDWARE) {
            val software = input.copy(Bitmap.Config.ARGB_8888, false) ?: return input
            return transform(software, size)
        }
        val w = input.width
        val h = input.height
        // Same margin as FaceEmbedder's crop
        val marginX = (xMax - xMin) * w * MARGIN_FRACTION
        val marginY = (yMax - yMin) * h * MARGIN_FRACTION
        val left = ((xMin * w) - marginX).toInt().coerceIn(0, w - 1)
        val top = ((yMin * h) - marginY).toInt().coerceIn(0, h - 1)
        val right = ((xMax * w) + marginX).toInt().coerceIn(left + 1, w)
        val bottom = ((yMax * h) + marginY).toInt().coerceIn(top + 1, h)
        return Bitmap.createBitmap(input, left, top, right - left, bottom - top)
    }

    companion object {
        private const val MARGIN_FRACTION = 0.1f
    }
}
