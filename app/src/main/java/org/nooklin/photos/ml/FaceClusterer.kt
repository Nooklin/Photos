package org.nooklin.photos.ml

import kotlin.math.sqrt

object FaceClusterer {
    const val MATCH_THRESHOLD = 0.5f

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

    fun serialize(embedding: FloatArray): String = embedding.joinToString(",")

    fun deserialize(serialized: String): FloatArray =
        serialized.split(",").map { it.toFloat() }.toFloatArray()
}
