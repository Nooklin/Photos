package org.nooklin.photos.ml

import android.graphics.Bitmap

// dHash: 9x8 grayscale, 64 bits from horizontal brightness comparisons
fun computeDHash(bitmap: Bitmap): Long {
    val width = 9
    val height = 8
    val resized = Bitmap.createScaledBitmap(bitmap, width, height, true)
    var hash = 0L
    var bitIndex = 0
    for (y in 0 until height) {
        for (x in 0 until width - 1) {
            val lumaLeft = luminance(resized.getPixel(x, y))
            val lumaRight = luminance(resized.getPixel(x + 1, y))
            if (lumaLeft < lumaRight) {
                hash = hash or (1L shl bitIndex)
            }
            bitIndex++
        }
    }
    if (resized !== bitmap) resized.recycle()
    return hash
}

private fun luminance(pixel: Int): Int {
    val r = (pixel shr 16) and 0xFF
    val g = (pixel shr 8) and 0xFF
    val b = pixel and 0xFF
    // BT.601 luma weights
    return (r * 299 + g * 587 + b * 114) / 1000
}

// 5 or fewer differing bits out of 64 counts as a duplicate
fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)
