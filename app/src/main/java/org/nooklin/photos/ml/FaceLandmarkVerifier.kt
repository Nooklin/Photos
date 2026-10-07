package org.nooklin.photos.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.math.max

// Second check per face: BlazeFace's 6 points can't tell a coherent pet face from a human one
class FaceLandmarkVerifier(context: Context) : AutoCloseable {
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

    // Axis-aligned crop: the rotated version rejected 83% of faces on device
    private fun alignedCrop(bitmap: Bitmap, box: FaceBox): Bitmap {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()

        val boxWidthPx = (box.xMax - box.xMin) * w
        val boxHeightPx = (box.yMax - box.yMin) * h
        val roiSize = (max(boxWidthPx, boxHeightPx) * ROI_SCALE).coerceAtLeast(1f)
        val cx = (box.xMin + box.xMax) / 2f * w
        val cy = (box.yMin + box.yMax) / 2f * h

        val output = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val matrix = Matrix()
        // Center on the box, scale to the model input, no rotation
        matrix.postTranslate(-cx, -cy)
        val scale = INPUT_SIZE / roiSize
        matrix.postScale(scale, scale)
        matrix.postTranslate(INPUT_SIZE / 2f, INPUT_SIZE / 2f)
        canvas.drawBitmap(bitmap, matrix, null)
        return output
    }

    private fun preprocess(crop: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        crop.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            // [0,1] input, the other bundled models use [-1,1]
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255f) // R
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255f) // G
            buffer.putFloat((pixel and 0xFF) / 255f) // B
        }
        buffer.rewind()
        return buffer
    }

    // Fails open: if it can't run, don't reject every face
    fun isConfirmedFace(bitmap: Bitmap, box: FaceBox): Boolean {
        return try {
            val crop = alignedCrop(bitmap, box)
            val input = preprocess(crop)
            crop.recycle()

            // Tensors matched by shape: landmarks has 478*3 values, presence is the other rank-4 tensor
            var landmarksIndex = -1
            var presenceIndex = -1
            var otherIndex = -1
            for (i in 0 until interpreter.outputTensorCount) {
                val shape = interpreter.getOutputTensor(i).shape()
                val elementCount = shape.fold(1) { acc, d -> acc * d }
                when {
                    elementCount == NUM_LANDMARKS * 3 -> landmarksIndex = i
                    elementCount == 1 && shape.size == 4 -> presenceIndex = i
                    elementCount == 1 -> otherIndex = i
                }
            }
            check(landmarksIndex >= 0 && presenceIndex >= 0 && otherIndex >= 0) {
                "Unexpected face_landmark output tensor shapes"
            }

            val landmarksOut = Array(1) { Array(1) { Array(1) { FloatArray(NUM_LANDMARKS * 3) } } }
            val presenceOut = Array(1) { Array(1) { Array(1) { FloatArray(1) } } }
            val otherOut = Array(1) { FloatArray(1) }
            interpreter.runForMultipleInputsOutputs(
                arrayOf(input),
                mapOf(landmarksIndex to landmarksOut, presenceIndex to presenceOut, otherIndex to otherOut)
            )

            val rawScore = presenceOut[0][0][0][0]
            val confidence = sigmoid(rawScore)
            // Score is logged every time, to tune the threshold
            scanLogD(FACE_SCAN_LOG_TAG, "FaceLandmarkVerifier confidence=$confidence (threshold=$PRESENCE_THRESHOLD)")
            confidence > PRESENCE_THRESHOLD
        } catch (e: Exception) {
            // Fails open, but logged so a broken verifier doesn't go unnoticed
            scanLogW(FACE_SCAN_LOG_TAG, "FaceLandmarkVerifier failed, treating as confirmed", e)
            true
        }
    }

    private fun sigmoid(x: Float): Float = 1f / (1f + exp(-x))

    override fun close() {
        // Don't load the model just to close it
        if (interpreterLazy.isInitialized()) interpreterLazy.value.close()
    }

    companion object {
        private const val MODEL_FILE_NAME = "face_landmark.tflite"
        private const val INPUT_SIZE = 256
        private const val NUM_LANDMARKS = 478
        private const val ROI_SCALE = 1.5f
        // 0.001, not MediaPipe's 0.5: 0.5 rejected real faces (0.015 to 0.227), a non-face scored 2e-9
        private const val PRESENCE_THRESHOLD = 0.001f
    }
}
