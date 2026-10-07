package org.nooklin.photos.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import android.graphics.Bitmap
import android.graphics.ImageDecoder

private const val SHARE_AUTHORITY = "org.nooklin.photos.fileprovider"

// Orientation is kept so photos aren't sideways. XMP, embedded thumbnails and serial/lens tags are not covered.
private val SHARE_STRIPPED_TAGS = listOf(
    ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
    ExifInterface.TAG_GPS_AREA_INFORMATION, ExifInterface.TAG_GPS_DATESTAMP,
    ExifInterface.TAG_GPS_DEST_LATITUDE, ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
    ExifInterface.TAG_GPS_DEST_LONGITUDE, ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
    ExifInterface.TAG_GPS_IMG_DIRECTION, ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
    ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
    ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
    ExifInterface.TAG_GPS_MAP_DATUM, ExifInterface.TAG_GPS_PROCESSING_METHOD,
    ExifInterface.TAG_GPS_SATELLITES, ExifInterface.TAG_GPS_SPEED,
    ExifInterface.TAG_GPS_SPEED_REF, ExifInterface.TAG_GPS_TIMESTAMP,
    ExifInterface.TAG_GPS_TRACK, ExifInterface.TAG_GPS_TRACK_REF,
    ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL, ExifInterface.TAG_SOFTWARE,
    ExifInterface.TAG_ARTIST, ExifInterface.TAG_COPYRIGHT,
    ExifInterface.TAG_IMAGE_DESCRIPTION, ExifInterface.TAG_USER_COMMENT,
    ExifInterface.TAG_MAKER_NOTE, ExifInterface.TAG_IMAGE_UNIQUE_ID,
    ExifInterface.TAG_DATETIME, ExifInterface.TAG_DATETIME_ORIGINAL,
    ExifInterface.TAG_DATETIME_DIGITIZED,
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

fun clearShareCache(context: Context) {
    File(context.cacheDir, "shared").listFiles()?.forEach { it.delete() }
}

// Fails closed: null if the tags can't be cleared (HEIC)
fun createExifStrippedShareUri(context: Context, item: MediaItem, stripExif: Boolean = true): Uri? {
    if (item.isVideo || !stripExif) return item.uri
    val extension = fileExtensionFor(item.mimeType, false)
    return copyToShareCache(context, extension, strip = true) { context.contentResolver.openInputStream(item.uri) }
}

fun createExifStrippedShareUriFromFile(
    context: Context,
    file: File,
    isVideo: Boolean,
    stripExif: Boolean = true,
    mimeType: String? = null
): Uri? {
    // Stored type wins, the file name can carry an old extension
    val extension = if (mimeType != null) fileExtensionFor(mimeType, isVideo) else file.extension.ifEmpty { "jpg" }
    return copyToShareCache(context, extension, strip = !isVideo && stripExif) { file.inputStream() }
}

private fun copyToShareCache(context: Context, extension: String, strip: Boolean, open: () -> InputStream?): Uri? {
    clearShareCache(context)
    val shareDir = File(context.cacheDir, "shared").apply { mkdirs() }
    var outFile = File(shareDir, "share_${System.currentTimeMillis()}.$extension")

    val copied = try {
        open()?.use { input -> outFile.outputStream().use { input.copyTo(it) } } != null
    } catch (e: Exception) {
        false
    }
    if (!copied) {
        outFile.delete()
        return null
    }
    if (strip && !clearExifTags(outFile)) {
        // HEIC can't be cleaned in place: share a JPEG, or nothing, never the original with its location
        val jpeg = convertToJpeg(outFile, shareDir)
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

private fun convertToJpeg(source: File, shareDir: File): File? = try {
    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(source)) { decoder, _, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    val target = File(shareDir, "share_${System.currentTimeMillis()}.jpg")
    val written = target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    bitmap.recycle()
    if (written) target else { target.delete(); null }
} catch (e: Exception) {
    null
} catch (e: OutOfMemoryError) {
    null
}

private fun clearExifTags(file: File): Boolean = try {
    val exif = ExifInterface(file.absolutePath)
    SHARE_STRIPPED_TAGS.forEach { exif.setAttribute(it, null) }
    exif.saveAttributes()
    true
} catch (e: Exception) {
    false
}
