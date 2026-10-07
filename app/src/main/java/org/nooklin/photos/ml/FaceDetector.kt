package org.nooklin.photos.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

data class FaceBox(
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float,
    val score: Float,
    val leftEyeX: Float,
    val leftEyeY: Float,
    val rightEyeX: Float,
    val rightEyeY: Float
)

private data class Point(val x: Float, val y: Float)

private data class FaceKeypoints(val leftEye: Point, val rightEye: Point, val noseTip: Point, val mouth: Point)

// SHORT_RANGE (~2m) misses distant faces, FULL_RANGE covers ~5m.
// Input size and anchors differ per model.
enum class FaceModel(
    val modelFileName: String,
    val inputSize: Int,
    val anchorConfig: BlazeFaceAnchors.SsdConfig,
    val expectedAnchorCount: Int
) {
    SHORT_RANGE(
        "blaze_face_short_range.tflite",
        128,
        BlazeFaceAnchors.SHORT_RANGE,
        BlazeFaceAnchors.SHORT_RANGE_ANCHOR_COUNT
    ),
    FULL_RANGE(
        "blaze_face_full_range.tflite",
        192,
        BlazeFaceAnchors.FULL_RANGE,
        BlazeFaceAnchors.FULL_RANGE_ANCHOR_COUNT
    )
}

class FaceDetector(context: Context, private val model: FaceModel) : AutoCloseable {
    private val appContext = context.applicationContext
    private val interpreterLazy = lazy { Interpreter(loadModelFile()) }
    private val interpreter: Interpreter get() = interpreterLazy.value
    private val anchors by lazy { BlazeFaceAnchors.generate(model.anchorConfig) }
    private val inputSize get() = model.inputSize
    private val scale get() = model.inputSize.toFloat()

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = appContext.assets.openFd(model.modelFileName)
        FileInputStream(assetFileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.declaredLength
            )
        }
    }

    // Plain stretch resize, box accuracy doesn't matter here
    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        for (pixel in pixels) {
            // [0,255] to [-1,1], the model's input range
            buffer.putFloat((((pixel shr 16) and 0xFF) / 127.5f) - 1.0f) // R
            buffer.putFloat((((pixel shr 8) and 0xFF) / 127.5f) - 1.0f) // G
            buffer.putFloat(((pixel and 0xFF) / 127.5f) - 1.0f) // B
        }
        buffer.rewind()
        // Only recycle a new bitmap, createScaledBitmap returns the same instance when nothing was scaled
        if (resized !== bitmap) resized.recycle()
        return buffer
    }

    fun detectFaceCount(bitmap: Bitmap): Int = detectFaces(bitmap).size

    fun detectFaces(bitmap: Bitmap): List<FaceBox> {
        val input = preprocess(bitmap)

        // Tensors matched by shape (last dim 16 = regressors, 1 = scores), not by index
        var regressorsIndex = -1
        var scoresIndex = -1
        for (i in 0 until interpreter.outputTensorCount) {
            val shape = interpreter.getOutputTensor(i).shape()
            val lastDim = shape.last()
            when (lastDim) {
                NUM_COORDS -> regressorsIndex = i
                1 -> scoresIndex = i
            }
        }
        check(regressorsIndex >= 0 && scoresIndex >= 0) {
            "Unexpected BlazeFace output tensor shapes for $model, expected one ending in $NUM_COORDS and one ending in 1"
        }

        val regressors = Array(1) { Array(model.expectedAnchorCount) { FloatArray(NUM_COORDS) } }
        val scores = Array(1) { Array(model.expectedAnchorCount) { FloatArray(1) } }
        val outputs = if (regressorsIndex == 0) {
            mapOf(0 to regressors, 1 to scores)
        } else {
            mapOf(0 to scores, 1 to regressors)
        }
        interpreter.runForMultipleInputsOutputs(arrayOf(input), outputs)

        val candidates = mutableListOf<FaceBox>()
        val nearMisses = mutableListOf<Float>()
        var geometryRejected = 0
        for (i in anchors.indices) {
            // Scale is the model input size, sigmoid scores (MediaPipe issue 4232)
            val rawScore = scores[0][i][0]
            val score = sigmoid(rawScore)
            if (score < MIN_SCORE_THRESHOLD) {
                // Near misses are logged: real faces scored 0.5 to 0.63
                if (score >= NEAR_MISS_LOG_FLOOR) nearMisses.add(score)
                continue
            }

            val anchor = anchors[i]
            val box = regressors[0][i]
            // Fixed anchor size: box size comes from the regression output
            val xCenter = box[0] / scale + anchor.xCenter
            val yCenter = box[1] / scale + anchor.yCenter
            val w = box[2] / scale
            val h = box[3] / scale

            val keypoints = decodeKeypoints(box, anchor)
            if (!isGeometricallyValid(keypoints, w)) {
                geometryRejected++
                continue
            }

            candidates.add(
                FaceBox(
                    xMin = xCenter - w / 2f,
                    yMin = yCenter - h / 2f,
                    xMax = xCenter + w / 2f,
                    yMax = yCenter + h / 2f,
                    score = score,
                    // Eye positions are kept for FaceLandmarkVerifier's crop
                    leftEyeX = keypoints.leftEye.x,
                    leftEyeY = keypoints.leftEye.y,
                    rightEyeX = keypoints.rightEye.x,
                    rightEyeY = keypoints.rightEye.y
                )
            )
        }

        if (nearMisses.isNotEmpty()) {
            val top = nearMisses.sortedDescending().take(5)
            scanLogD(FACE_SCAN_LOG_TAG, "$model: ${nearMisses.size} near-miss(es) below $MIN_SCORE_THRESHOLD, top scores: $top")
        }
        if (geometryRejected > 0) {
            scanLogD(FACE_SCAN_LOG_TAG, "$model: $geometryRejected candidate(s) above score threshold but rejected by landmark geometry check")
        }

        return nonMaxSuppress(candidates, NMS_IOU_THRESHOLD)
    }

    // The 6 landmarks BlazeFace outputs next to the box (offset 4)
    private fun decodeKeypoints(box: FloatArray, anchor: Anchor): FaceKeypoints {
        fun point(index: Int): Point {
            val x = box[4 + index * 2] / scale + anchor.xCenter
            val y = box[4 + index * 2 + 1] / scale + anchor.yCenter
            return Point(x, y)
        }
        return FaceKeypoints(
            leftEye = point(0),
            rightEye = point(1),
            noseTip = point(2),
            mouth = point(3)
        )
    }

    // Landmark geometry check: rejects false positives whose score overlaps real faces
    private fun isGeometricallyValid(kp: FaceKeypoints, boxWidth: Float): Boolean {
        val eyeDx = kp.rightEye.x - kp.leftEye.x
        val eyeDy = kp.rightEye.y - kp.leftEye.y
        val eyeDist = kotlin.math.sqrt(eyeDx * eyeDx + eyeDy * eyeDy)
        if (eyeDist <= 1e-6f) return false

        if (abs(eyeDy) > eyeDist * 0.6f) return false

        val eyeFraction = eyeDist / boxWidth.coerceAtLeast(1e-6f)
        if (eyeFraction < 0.15f || eyeFraction > 0.75f) return false

        val eyeMidY = (kp.leftEye.y + kp.rightEye.y) / 2f
        if (kp.noseTip.y < eyeMidY - eyeDist * 0.1f) return false
        if (kp.mouth.y < kp.noseTip.y - eyeDist * 0.1f) return false

        val minEyeX = min(kp.leftEye.x, kp.rightEye.x)
        val maxEyeX = max(kp.leftEye.x, kp.rightEye.x)
        val noseMargin = eyeDist * 0.5f
        if (kp.noseTip.x < minEyeX - noseMargin || kp.noseTip.x > maxEyeX + noseMargin) return false

        return true
    }

    private fun sigmoid(x: Float): Float = 1f / (1f + exp(-x))

    override fun close() {
        // Don't load the model just to close it
        if (interpreterLazy.isInitialized()) interpreterLazy.value.close()
    }

    companion object {
        private const val NUM_COORDS = 16 // 4 box + 6 keypoints * 2
        // 0.5: real faces scored 0.5 to 0.63. Precision comes from the geometry check.
        private const val MIN_SCORE_THRESHOLD = 0.5f
        // Floor for the near-miss log
        private const val NEAR_MISS_LOG_FLOOR = 0.3f
        // Per-model NMS, the cross-model merge uses MERGE_IOU_THRESHOLD
        private const val NMS_IOU_THRESHOLD = 0.3f
        // Stricter than NMS_IOU_THRESHOLD: the lenient one merged two people standing close together
        private const val MERGE_IOU_THRESHOLD = 0.5f
        private fun nonMaxSuppress(boxes: List<FaceBox>, iouThreshold: Float): List<FaceBox> {
            val sorted = boxes.sortedByDescending { it.score }.toMutableList()
            val kept = mutableListOf<FaceBox>()
            while (sorted.isNotEmpty()) {
                val best = sorted.removeAt(0)
                kept.add(best)
                sorted.removeAll { iou(best, it) > iouThreshold }
            }
            return kept
        }

        private fun iou(a: FaceBox, b: FaceBox): Float {
            val interXMin = max(a.xMin, b.xMin)
            val interYMin = max(a.yMin, b.yMin)
            val interXMax = min(a.xMax, b.xMax)
            val interYMax = min(a.yMax, b.yMax)
            val interArea = max(0f, interXMax - interXMin) * max(0f, interYMax - interYMin)
            val areaA = max(0f, a.xMax - a.xMin) * max(0f, a.yMax - a.yMin)
            val areaB = max(0f, b.xMax - b.xMin) * max(0f, b.yMax - b.yMin)
            val union = areaA + areaB - interArea
            return if (union <= 0f) 0f else interArea / union
        }

        fun mergeFaceBoxes(a: List<FaceBox>, b: List<FaceBox>): List<FaceBox> {
            val kept = nonMaxSuppress(a + b, MERGE_IOU_THRESHOLD)
            // Drop the lower score when one box sits almost inside another (a 0.12 box inside a 0.19 box gave two people)
            val result = mutableListOf<FaceBox>()
            for (box in kept.sortedByDescending { it.score }) {
                if (result.none { containedIn(box, it) || containedIn(it, box) }) result.add(box)
            }
            return result
        }

        private fun containedIn(inner: FaceBox, outer: FaceBox): Boolean {
            val interArea = max(0f, min(inner.xMax, outer.xMax) - max(inner.xMin, outer.xMin)) *
                max(0f, min(inner.yMax, outer.yMax) - max(inner.yMin, outer.yMin))
            val innerArea = max(0f, inner.xMax - inner.xMin) * max(0f, inner.yMax - inner.yMin)
            return innerArea > 0f && interArea / innerArea > MERGE_CONTAINMENT_THRESHOLD
        }

        private const val MERGE_CONTAINMENT_THRESHOLD = 0.8f
    }
}
