package org.nooklin.photos.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

// 192-dim embedding per face, compared by Euclidean distance
class FaceEmbedder(context: Context) : AutoCloseable {
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

    // Crop with a margin around the box, resized to 112x112
    fun embed(bitmap: Bitmap, box: FaceBox): FloatArray {
        val w = bitmap.width
        val h = bitmap.height
        val marginX = (box.xMax - box.xMin) * w * MARGIN_FRACTION
        val marginY = (box.yMax - box.yMin) * h * MARGIN_FRACTION
        val left = ((box.xMin * w) - marginX).toInt().coerceIn(0, w - 1)
        val top = ((box.yMin * h) - marginY).toInt().coerceIn(0, h - 1)
        val right = ((box.xMax * w) + marginX).toInt().coerceIn(left + 1, w)
        val bottom = ((box.yMax * h) + marginY).toInt().coerceIn(top + 1, h)

        val cropped = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        val resized = Bitmap.createScaledBitmap(cropped, INPUT_SIZE, INPUT_SIZE, true)
        if (resized !== cropped) cropped.recycle()

        val buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            // (pixel - 128) / 128: the input range of this converted model
            buffer.putFloat((((pixel shr 16) and 0xFF) - 128) / 128f) // R
            buffer.putFloat((((pixel shr 8) and 0xFF) - 128) / 128f) // G
            buffer.putFloat(((pixel and 0xFF) - 128) / 128f) // B
        }
        buffer.rewind()
        resized.recycle()

        val output = Array(1) { FloatArray(EMBEDDING_SIZE) }
        interpreter.run(buffer, output)
        return output[0]
    }

    override fun close() {
        // Don't load the model just to close it
        if (interpreterLazy.isInitialized()) interpreterLazy.value.close()
    }

    companion object {
        private const val MODEL_FILE_NAME = "mobilefacenet.tflite"
        private const val INPUT_SIZE = 112
        private const val EMBEDDING_SIZE = 192
        private const val MARGIN_FRACTION = 0.1f
    }
}
