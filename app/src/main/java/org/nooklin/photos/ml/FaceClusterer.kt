package org.nooklin.photos.ml

import kotlin.math.sqrt

object FaceClusterer {
    // Euclidean distance, 0.5 is the standard for this model. Lower splits people, higher merges them.
    const val MATCH_THRESHOLD = 0.5f

    // A bit above MATCH_THRESHOLD, averages are steadier. Raise to combine more.
    const val COMBINE_THRESHOLD = 0.55f

    fun distance(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return Float.MAX_VALUE
        var sum = 0f
        for (i in a.indices) {
            val d = a[i] - b[i]
            sum += d * d
        }
        return sqrt(sum)
    }

    // String column: avoids FloatArray's reference equality in Room entities
    fun serialize(embedding: FloatArray): String = embedding.joinToString(",")

    fun deserialize(serialized: String): FloatArray =
        serialized.split(",").map { it.toFloat() }.toFloatArray()
}
