package org.nooklin.photos.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class DetectedObject(
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float,
    val score: Float,
    val cocoClassIndex: Int
)

// EfficientDet-Lite0 (COCO), a general object detector next to the face-only BlazeFace
class ObjectDetector(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val interpreterLazy = lazy { Interpreter(loadModelFile()) }
    private val interpreter: Interpreter get() = interpreterLazy.value

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = appContext.assets.openFd(MODEL_FILE_NAME)
        FileInputStream(assetFileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.declaredLength
            )
        }
    }

    // uint8 model: raw 0-255 bytes, no normalization
    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val buffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            buffer.put(((pixel shr 16) and 0xFF).toByte()) // R
            buffer.put(((pixel shr 8) and 0xFF).toByte()) // G
            buffer.put((pixel and 0xFF).toByte()) // B
        }
        buffer.rewind()
        if (resized !== bitmap) resized.recycle()
        return buffer
    }

    // minScore 0.4 is a starting point, not device-verified
    fun detect(bitmap: Bitmap, minScore: Float = DEFAULT_MIN_SCORE): List<DetectedObject> {
        val input = preprocess(bitmap)

        var boxesIndex = -1
        var classesIndex = -1
        var scoresIndex = -1
        var countIndex = -1
        for (i in 0 until interpreter.outputTensorCount) {
            val shape = interpreter.getOutputTensor(i).shape()
            when {
                shape.size == 3 && shape.last() == 4 -> boxesIndex = i
                shape.size == 1 -> countIndex = i
                // classes and scores share a shape, told apart by value in the swap check
                shape.size == 2 && classesIndex == -1 -> classesIndex = i
                shape.size == 2 -> scoresIndex = i
            }
        }
        check(boxesIndex >= 0 && classesIndex >= 0 && scoresIndex >= 0 && countIndex >= 0) {
            "Unexpected EfficientDet-Lite0 output tensor shapes, expected boxes/classes/scores/count"
        }

        val maxDetections = interpreter.getOutputTensor(boxesIndex).shape()[1]
        val boxes = Array(1) { Array(maxDetections) { FloatArray(4) } }
        val classesRaw = Array(1) { FloatArray(maxDetections) }
        val scoresRaw = Array(1) { FloatArray(maxDetections) }
        // The count tensor is rank 1 ([1]), wrapping it in arrayOf crashed copyTo
        val count = FloatArray(1)
        val outputs = mutableMapOf<Int, Any>(
            boxesIndex to boxes,
            classesIndex to classesRaw,
            scoresIndex to scoresRaw,
            countIndex to count
        )
        interpreter.runForMultipleInputsOutputs(arrayOf(input), outputs)

        // Swap if the output order was guessed wrong: classes are integers, scores are 0 to 1.
        // Several slots are checked, one could be 0.0 or 1.0 by chance.
        val looksLikeScore = classesRaw[0].take(SWAP_CHECK_SAMPLE_SIZE)
            .any { it in 0f..1f && it != 0f && it != 1f }
        val (classesArr, scoresArr) = if (looksLikeScore) scoresRaw[0] to classesRaw[0] else classesRaw[0] to scoresRaw[0]

        val results = mutableListOf<DetectedObject>()
        val validCount = count[0].toInt().coerceIn(0, maxDetections)

        // Diagnostic: tells a threshold miss apart from a recall gap for cats and dogs
        for (i in 0 until maxDetections) {
            val score = scoresArr[i]
            val classIndex = classesArr[i].toInt()
            // score > 0 skips padding slots, which would log as a near miss on every photo
            if (score > 0f && score < minScore && classIndex in TRACKED_CLASSES) {
                scanLogD(OBJECT_SCAN_LOG_TAG, "near-miss: class=$classIndex score=$score (below $minScore)")
            }
        }

        for (i in 0 until validCount) {
            val score = scoresArr[i]
            val classIndex = classesArr[i].toInt()
            // Pets get a lower threshold: a dog scored 0.129 against 0.04 to 0.07 noise, under the 0.4 cutoff
            val effectiveMinScore = if (classIndex == CocoClass.CAT || classIndex == CocoClass.DOG) {
                PET_MIN_SCORE
            } else {
                minScore
            }
            if (score < effectiveMinScore) continue
            val box = boxes[0][i]
            // [yMin, xMin, yMax, xMax]
            results.add(
                DetectedObject(
                    xMin = box[1],
                    yMin = box[0],
                    xMax = box[3],
                    yMax = box[2],
                    score = score,
                    cocoClassIndex = classIndex
                )
            )
        }
        return results
    }

    override fun close() {
        // Don't load the model just to close it
        if (interpreterLazy.isInitialized()) interpreterLazy.value.close()
    }

    companion object {
        private const val MODEL_FILE_NAME = "efficientdet_lite0.tflite"
        private const val INPUT_SIZE = 320
        private const val DEFAULT_MIN_SCORE = 0.4f
        // Slots the swap check samples
        private const val SWAP_CHECK_SAMPLE_SIZE = 5
        // Above the 0.04 to 0.07 noise, below the real 0.129 dog
        private const val PET_MIN_SCORE = 0.1f
        // Keep in sync with CocoClass by hand
        private val TRACKED_CLASSES = setOf(CocoClass.PERSON, CocoClass.CAR, CocoClass.MOTORCYCLE, CocoClass.CAT, CocoClass.DOG)
    }
}
