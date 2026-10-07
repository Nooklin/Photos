package org.nooklin.photos.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.MediaStore
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// No re-encode, so the cut starts on the keyframe before the chosen start
object VideoClipExporter {
    suspend fun export(context: Context, source: Uri, startMs: Long, endMs: Long): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, ImageNaming.videoFileName(context, "mp4"))
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val target = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        try {
            val descriptor = resolver.openFileDescriptor(target, "rw") ?: throw IllegalStateException("No file")
            descriptor.use { copyRange(context, source, startMs, endMs, it.fileDescriptor) }
            resolver.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            target
        } catch (e: Exception) {
            resolver.delete(target, null, null)
            null
        }
    }

    private fun copyRange(context: Context, source: Uri, startMs: Long, endMs: Long, output: java.io.FileDescriptor) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        try {
            extractor.setDataSource(context, source, null)
            muxer = MediaMuxer(output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackMap = HashMap<Int, Int>()
            var bufferSize = 1 shl 20
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (!mime.startsWith("video/") && !mime.startsWith("audio/")) continue
                extractor.selectTrack(index)
                trackMap[index] = muxer.addTrack(format)
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    bufferSize = maxOf(bufferSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                }
            }
            if (trackMap.isEmpty()) throw IllegalStateException("No tracks")

            // Portrait stays upright: only the rotation flag is copied
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, source)
                val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                muxer.setOrientationHint(rotation)
            } finally {
                retriever.release()
            }

            muxer.start()
            muxerStarted = true

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val buffer = ByteBuffer.allocate(bufferSize)
            val info = android.media.MediaCodec.BufferInfo()
            var baseUs = -1L
            while (true) {
                buffer.clear()
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val time = extractor.sampleTime
                if (time > endUs) break
                if (baseUs < 0L) baseUs = time
                val outTrack = trackMap[extractor.sampleTrackIndex]
                if (outTrack != null) {
                    info.set(0, size, (time - baseUs).coerceAtLeast(0L), extractor.sampleFlags)
                    muxer.writeSampleData(outTrack, buffer, info)
                }
                extractor.advance()
            }
        } finally {
            if (muxerStarted) {
                try { muxer?.stop() } catch (e: Exception) { /* nothing was written, the caller deletes the file */ }
            }
            try { muxer?.release() } catch (e: Exception) { /* already released */ }
            extractor.release()
        }
    }
}
