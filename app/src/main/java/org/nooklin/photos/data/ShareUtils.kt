package org.nooklin.photos.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.nooklin.photos.data.local.AppPreferences
import org.nooklin.photos.data.local.ShareStripOptions
import java.nio.ByteBuffer

private const val SHARE_AUTHORITY = "org.nooklin.photos.fileprovider"

// Exif tags per kind of data, as chosen in Settings, Sharing. Orientation is never removed so photos aren't sideways.
private val LOCATION_TAGS = listOf(
    ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
    ExifInterface.TAG_GPS_AREA_INFORMATION,
    ExifInterface.TAG_GPS_DEST_LATITUDE, ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
    ExifInterface.TAG_GPS_DEST_LONGITUDE, ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
    ExifInterface.TAG_GPS_IMG_DIRECTION, ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
    ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
    ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
    ExifInterface.TAG_GPS_MAP_DATUM, ExifInterface.TAG_GPS_PROCESSING_METHOD,
    ExifInterface.TAG_GPS_SATELLITES, ExifInterface.TAG_GPS_SPEED,
    ExifInterface.TAG_GPS_SPEED_REF, ExifInterface.TAG_GPS_TRACK, ExifInterface.TAG_GPS_TRACK_REF
)

private val DATE_TIME_TAGS = listOf(
    ExifInterface.TAG_DATETIME, ExifInterface.TAG_DATETIME_ORIGINAL, ExifInterface.TAG_DATETIME_DIGITIZED,
    ExifInterface.TAG_GPS_DATESTAMP, ExifInterface.TAG_GPS_TIMESTAMP,
    ExifInterface.TAG_SUBSEC_TIME, ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
    ExifInterface.TAG_OFFSET_TIME, ExifInterface.TAG_OFFSET_TIME_ORIGINAL, ExifInterface.TAG_OFFSET_TIME_DIGITIZED
)

private val CAMERA_TAGS = listOf(
    ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL, ExifInterface.TAG_SOFTWARE,
    ExifInterface.TAG_LENS_MAKE, ExifInterface.TAG_LENS_MODEL, ExifInterface.TAG_LENS_SERIAL_NUMBER,
    ExifInterface.TAG_BODY_SERIAL_NUMBER,
    ExifInterface.TAG_EXPOSURE_TIME, ExifInterface.TAG_F_NUMBER,
    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, ExifInterface.TAG_FOCAL_LENGTH,
    ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, ExifInterface.TAG_FLASH,
    ExifInterface.TAG_WHITE_BALANCE, ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
    ExifInterface.TAG_METERING_MODE, ExifInterface.TAG_EXPOSURE_PROGRAM,
    ExifInterface.TAG_APERTURE_VALUE, ExifInterface.TAG_SHUTTER_SPEED_VALUE,
    ExifInterface.TAG_BRIGHTNESS_VALUE, ExifInterface.TAG_EXPOSURE_MODE,
    ExifInterface.TAG_LIGHT_SOURCE, ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
    ExifInterface.TAG_SCENE_CAPTURE_TYPE
)

private val DESCRIPTION_TAGS = listOf(
    ExifInterface.TAG_ARTIST, ExifInterface.TAG_COPYRIGHT, ExifInterface.TAG_IMAGE_DESCRIPTION,
    ExifInterface.TAG_USER_COMMENT, ExifInterface.TAG_MAKER_NOTE, ExifInterface.TAG_IMAGE_UNIQUE_ID,
    ExifInterface.TAG_CAMERA_OWNER_NAME
)

private fun tagsFor(options: ShareStripOptions): List<String> =
    (if (options.location) LOCATION_TAGS else emptyList()) +
        (if (options.dateTime) DATE_TIME_TAGS else emptyList()) +
        (if (options.camera) CAMERA_TAGS else emptyList()) +
        (if (options.description) DESCRIPTION_TAGS else emptyList())

// Read when a share is made, these functions run off the main thread
private fun stripOptions(context: Context): ShareStripOptions =
    runBlocking { AppPreferences(context).shareStripOptions.first() }

fun clearShareCache(context: Context) {
    File(context.cacheDir, "shared").listFiles()?.forEach { it.delete() }
}

// Fails closed: null if the tags can't be cleared (HEIC)
fun createExifStrippedShareUri(context: Context, item: MediaItem, stripExif: Boolean = true, cancelled: () -> Boolean = { false }): Uri? {
    if (!stripExif) return item.uri
    val options = stripOptions(context)
    // Remove Exif data is on but every kind is switched off: nothing to remove
    if (!options.any) return item.uri
    val extension = fileExtensionFor(item.mimeType, item.isVideo)
    return copyToShareCache(context, extension, strip = true, isVideo = item.isVideo, options = options, cancelled = cancelled) { context.contentResolver.openInputStream(item.uri) }
}

fun createExifStrippedShareUriFromFile(
    context: Context,
    file: File,
    isVideo: Boolean,
    stripExif: Boolean = true,
    mimeType: String? = null,
    cancelled: () -> Boolean = { false }
): Uri? {
    // Stored type wins, the file name can carry an old extension
    val extension = if (mimeType != null) fileExtensionFor(mimeType, isVideo) else file.extension.ifEmpty { "jpg" }
    val options = stripOptions(context)
    return copyToShareCache(context, extension, strip = stripExif && options.any, isVideo = isVideo, options = options, cancelled = cancelled) { file.inputStream() }
}

private fun copyToShareCache(
    context: Context,
    extension: String,
    strip: Boolean,
    // Several files at once clear the cache once and give each file its own name
    clearCache: Boolean = true,
    baseName: String = "share_${System.currentTimeMillis()}",
    isVideo: Boolean = false,
    options: ShareStripOptions = ShareStripOptions(),
    cancelled: () -> Boolean = { false },
    open: () -> InputStream?
): Uri? {
    if (clearCache) clearShareCache(context)
    val shareDir = File(context.cacheDir, "shared").apply { mkdirs() }
    var outFile = File(shareDir, "$baseName.$extension")

    val copied = try {
        open()?.use { input ->
            outFile.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    if (cancelled()) throw java.io.IOException("cancelled")
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                }
            }
        } != null
    } catch (e: Exception) {
        false
    }
    if (!copied) {
        outFile.delete()
        return null
    }
    if (strip && isVideo) {
        // Copied again without the metadata, or nothing: never the original with its location
        val clean = File(shareDir, "${baseName}_clean.mp4")
        val ok = removeVideoMetadata(outFile, clean, cancelled)
        outFile.delete()
        if (!ok) {
            clean.delete()
            return null
        }
        outFile = clean
    } else if (strip && !clearMetadata(outFile, options)) {
        // HEIC can't be cleaned in place: share a JPEG, or nothing, never the original with its location
        val jpeg = convertToJpeg(outFile, shareDir, "${baseName}_jpeg")
        outFile.delete()
        if (jpeg == null) return null
        outFile = jpeg
    }

    return try {
        FileProvider.getUriForFile(context, SHARE_AUTHORITY, outFile)
    } catch (e: Exception) {
        outFile.delete()
        null
    }
}

private fun convertToJpeg(source: File, shareDir: File, baseName: String): File? = try {
    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(source)) { decoder, _, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    val target = File(shareDir, "$baseName.jpg")
    val written = target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    bitmap.recycle()
    if (written) target else { target.delete(); null }
} catch (e: Exception) {
    null
} catch (e: OutOfMemoryError) {
    null
}

private fun clearMetadata(file: File, options: ShareStripOptions): Boolean {
    val isJpeg = file.inputStream().use { it.read() == 0xFF && it.read() == 0xD8 }
    return if (isJpeg) stripJpegMetadata(file, options) else clearExifTags(file, options)
}

// JPEG, without touching the picture data (no new compression). With every kind switched on, all segments that can hold
// personal data are cut out: Exif (location, camera, date, thumbnail), XMP, IPTC, comments, maker notes and anything
// added after the image end (for example the video of a motion photo). Kept: what is needed to draw the picture, the
// colour profile and the orientation. With only some kinds on, the Exif block stays and only the chosen tags are removed
// afterwards, XMP and IPTC go when location, date or description is on (they can hold those too, and cannot be split per
// kind). False if the file is not a clean JPEG, then the caller makes a metadata free copy another way.
private fun stripJpegMetadata(file: File, options: ShareStripOptions): Boolean {
    try {
    val orientation = ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
    val b = file.readBytes()
    val n = b.size
    val out = ByteArrayOutputStream(n)
    out.write(0xFF)
    out.write(0xD8)
    var i = 2
    var finished = false
    while (i + 1 < n) {
        if ((b[i].toInt() and 0xFF) != 0xFF) return false
        var marker = b[i + 1].toInt() and 0xFF
        // Fill bytes before a marker
        while (marker == 0xFF && i + 2 < n) {
            i++
            marker = b[i + 1].toInt() and 0xFF
        }
        if (marker == 0xD9) {
            out.write(0xFF)
            out.write(0xD9)
            finished = true
            break
        }
        // Markers without a length
        if (marker == 0x01 || marker in 0xD0..0xD7) {
            out.write(b, i, 2)
            i += 2
            continue
        }
        if (i + 3 >= n) return false
        val length = ((b[i + 2].toInt() and 0xFF) shl 8) or (b[i + 3].toInt() and 0xFF)
        val end = i + 2 + length
        if (length < 2 || end > n) return false
        if (keepJpegSegment(marker, b, i + 4, end, options)) out.write(b, i, end - i)
        i = end
        if (marker == 0xDA) {
            // Picture data runs until the next real marker (FF followed by something other than 00 or a restart)
            var j = i
            while (j + 1 < n) {
                val next = b[j + 1].toInt() and 0xFF
                if ((b[j].toInt() and 0xFF) == 0xFF && next != 0x00 && next !in 0xD0..0xD7) break
                j++
            }
            if (j + 1 >= n) return false
            out.write(b, i, j - i)
            i = j
        }
    }
    if (!finished) return false
    file.writeBytes(out.toByteArray())
    if (options.all) {
        // The picture would be sideways without it, ExifInterface writes a new Exif block with only this value
        if (orientation > ExifInterface.ORIENTATION_NORMAL) {
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            exif.saveAttributes()
        }
    } else if (!clearExifTags(file, options)) {
        return false
    }
    return true
    } catch (e: Exception) {
        return false
    }
}

private fun keepJpegSegment(marker: Int, b: ByteArray, dataStart: Int, end: Int, options: ShareStripOptions): Boolean {
    fun startsWith(text: String): Boolean {
        if (dataStart + text.length > end) return false
        return text.indices.all { b[dataStart + it].toInt() == text[it].code }
    }
    // XMP and IPTC can hold location, date and description
    val dropXmpAndIptc = options.all || options.location || options.dateTime || options.description
    return when (marker) {
        // APP0: JFIF header yes, JFXX (extension with a thumbnail) no
        0xE0 -> startsWith("JFIF")
        // APP1: the Exif block stays when only some kinds are removed (its tags are removed afterwards), XMP goes
        0xE1 -> !options.all && (if (startsWith("Exif")) true else !dropXmpAndIptc)
        // APP2: colour profile yes, other (FlashPix, multi picture) no
        0xE2 -> startsWith("ICC_PROFILE")
        // APP13: IPTC and Photoshop data
        0xED -> !options.all && !dropXmpAndIptc
        // APP14: Adobe colour transform, needed to draw some JPEGs correctly
        0xEE -> startsWith("Adobe")
        // Comments
        0xFE -> !options.all && !options.description
        // Other APP segments (vendor data)
        in 0xE3..0xEF -> false
        else -> true
    }
}

// Copies the audio and video tracks into a new MP4 without recompressing. A new MP4 only holds what is written to it,
// so location and other tags of the original are not carried over. False if a track cannot be copied.
private fun removeVideoMetadata(source: File, target: File, cancelled: () -> Boolean = { false }): Boolean {
    val extractor = MediaExtractor()
    var muxer: MediaMuxer? = null
    var started = false
    return try {
        extractor.setDataSource(source.absolutePath)
        val retriever = MediaMetadataRetriever()
        val rotation = try {
            retriever.setDataSource(source.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        } finally {
            retriever.release()
        }
        muxer = MediaMuxer(target.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val trackMap = HashMap<Int, Int>()
        for (index in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                trackMap[index] = muxer.addTrack(format)
                extractor.selectTrack(index)
            }
        }
        if (trackMap.isEmpty()) return false
        muxer.setOrientationHint(rotation)
        muxer.start()
        started = true
        val buffer = ByteBuffer.allocate(8 * 1024 * 1024)
        val info = android.media.MediaCodec.BufferInfo()
        while (true) {
            if (cancelled()) return false
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            val flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
            info.set(0, size, extractor.sampleTime, flags)
            muxer.writeSampleData(trackMap.getValue(extractor.sampleTrackIndex), buffer, info)
            extractor.advance()
        }
        muxer.stop()
        true
    } catch (e: Exception) {
        false
    } finally {
        extractor.release()
        runCatching { muxer?.release() }
        if (!started) target.delete()
    }
}

private fun clearExifTags(file: File, options: ShareStripOptions): Boolean = try {
    val exif = ExifInterface(file.absolutePath)
    tagsFor(options).forEach { exif.setAttribute(it, null) }
    exif.saveAttributes()
    true
} catch (e: Exception) {
    false
}

// Several items for one share, save or copy. An item whose metadata cannot be removed gets a null Uri (skipped, never
// passed on with its location). Videos are copied without their metadata too.
fun createShareUris(context: Context, items: List<MediaItem>, stripExif: Boolean, cancelled: () -> Boolean = { false }): List<Pair<MediaItem, Uri?>> {
    clearShareCache(context)
    val options = stripOptions(context)
    return items.mapIndexed { index, item ->
        if (cancelled()) return@mapIndexed item to null
        if (!stripExif || !options.any) {
            item to item.uri
        } else {
            val extension = fileExtensionFor(item.mimeType, item.isVideo)
            item to copyToShareCache(context, extension, strip = true, clearCache = false, baseName = "share_${index}_${System.nanoTime()}", isVideo = item.isVideo, options = options, cancelled = cancelled) {
                context.contentResolver.openInputStream(item.uri)
            }
        }
    }
}
