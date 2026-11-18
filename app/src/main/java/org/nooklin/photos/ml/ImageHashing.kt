package org.nooklin.photos.ml

import android.graphics.Bitmap

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
    return (r * 299 + g * 587 + b * 114) / 1000
}

fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

fun <K> groupSimilarHashes(items: List<Pair<K, Long>>, threshold: Int = 5): List<List<K>> {
    val parent = HashMap<K, K>()
    fun find(x: K): K {
        var root = x
        while (true) {
            val next = parent[root] ?: break
            if (next == root) break
            root = next
        }
        return root
    }
    val matched = LinkedHashSet<K>()
    var shift = 0
    for (bits in intArrayOf(11, 11, 11, 11, 10, 10)) {
        val mask = (1L shl bits) - 1
        for (bucket in items.groupBy { (it.second ushr shift) and mask }.values) {
            for (i in bucket.indices) {
                for (j in i + 1 until bucket.size) {
                    if (hammingDistance(bucket[i].second, bucket[j].second) <= threshold) {
                        val a = find(bucket[i].first)
                        val b = find(bucket[j].first)
                        if (a != b) parent[a] = b
                        matched.add(bucket[i].first)
                        matched.add(bucket[j].first)
                    }
                }
            }
        }
        shift += bits
    }
    return matched.groupBy { find(it) }.values.sortedByDescending { it.size }
}
