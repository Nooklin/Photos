package org.nooklin.photos.ml

import kotlin.math.ceil
import kotlin.math.sqrt

data class Anchor(val xCenter: Float, val yCenter: Float)

// Rebuilds the anchor grid the model was trained against (MediaPipe port).
// One parameterized generator, no copies. SHORT_RANGE gives 896 anchors, FULL_RANGE 2304.
object BlazeFaceAnchors {
    data class SsdConfig(
        val numLayers: Int,
        val inputSize: Int,
        val strides: IntArray,
        val interpolatedScaleAspectRatio: Float,
        val minScale: Float = 0.1484375f,
        val maxScale: Float = 0.75f,
        val anchorOffsetX: Float = 0.5f,
        val anchorOffsetY: Float = 0.5f,
        val aspectRatios: FloatArray = floatArrayOf(1.0f)
    )

    val SHORT_RANGE = SsdConfig(
        numLayers = 4,
        inputSize = 128,
        strides = intArrayOf(8, 16, 16, 16),
        interpolatedScaleAspectRatio = 1.0f
    )
    const val SHORT_RANGE_ANCHOR_COUNT = 896

    val FULL_RANGE = SsdConfig(
        numLayers = 1,
        inputSize = 192,
        strides = intArrayOf(4),
        interpolatedScaleAspectRatio = 0.0f
    )
    const val FULL_RANGE_ANCHOR_COUNT = 2304

    private fun calculateScale(config: SsdConfig, strideIndex: Int, numStrides: Int): Float {
        return if (numStrides == 1) {
            (config.minScale + config.maxScale) * 0.5f
        } else {
            config.minScale + (config.maxScale - config.minScale) * strideIndex / (numStrides - 1).toFloat()
        }
    }

    fun generate(config: SsdConfig): List<Anchor> {
        val anchors = mutableListOf<Anchor>()
        var layerId = 0
        while (layerId < config.numLayers) {
            val aspectRatios = mutableListOf<Float>()
            val scales = mutableListOf<Float>()

            var lastSameStrideLayer = layerId
            while (lastSameStrideLayer < config.numLayers && config.strides[lastSameStrideLayer] == config.strides[layerId]) {
                val scale = calculateScale(config, lastSameStrideLayer, config.numLayers)
                for (aspectRatio in config.aspectRatios) {
                    aspectRatios.add(aspectRatio)
                    scales.add(scale)
                }
                if (config.interpolatedScaleAspectRatio > 0f) {
                    val scaleNext = if (lastSameStrideLayer == config.numLayers - 1) {
                        1.0f
                    } else {
                        calculateScale(config, lastSameStrideLayer + 1, config.numLayers)
                    }
                    scales.add(sqrt(scale * scaleNext))
                    aspectRatios.add(config.interpolatedScaleAspectRatio)
                }
                lastSameStrideLayer++
            }

            val stride = config.strides[layerId]
            val featureMapSize = ceil(config.inputSize.toFloat() / stride).toInt()
            val anchorsPerPosition = aspectRatios.size

            for (y in 0 until featureMapSize) {
                for (x in 0 until featureMapSize) {
                    repeat(anchorsPerPosition) {
                        val xCenter = (x + config.anchorOffsetX) / featureMapSize
                        val yCenter = (y + config.anchorOffsetY) / featureMapSize
                        // fixed_anchor_size: box size comes from the model output, not the anchors
                        anchors.add(Anchor(xCenter, yCenter))
                    }
                }
            }
            layerId = lastSameStrideLayer
        }
        return anchors
    }
}
