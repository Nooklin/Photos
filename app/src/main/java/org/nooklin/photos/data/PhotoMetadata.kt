package org.nooklin.photos.data

import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// EXIF only, null when absent. No guessed lens names.
data class PhotoMetadata(
    val name: String,
    val sizeBytes: Long,
    val weekdayDateLabel: String,
    val timeLabel: String,
    val dimensions: String?,
    val megapixels: String?,
    val format: String?,
    val cameraMake: String?,
    val cameraModel: String?,
    val iso: String?,
    val focalLength: String?,
    val aperture: String?,
    val shutterSpeed: String?,
    val gpsCoordinates: String?
)

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
}

fun formatDayMonth(millis: Long): String =
    SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(millis))

fun formatTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

private fun formatWeekdayDate(millis: Long): String =
    SimpleDateFormat("EEEE \u2022 d MMM yyyy", Locale.getDefault()).format(Date(millis))

fun readExif(open: () -> InputStream?): ExifInterface? =
    try {
        open()?.use { ExifInterface(it) }
    } catch (e: Exception) {
        null
    }

fun readExif(file: File): ExifInterface? =
    try {
        if (file.exists()) ExifInterface(file.absolutePath) else null
    } catch (e: Exception) {
        null
    }

private fun ExifInterface.readIso(): String? =
    getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" }

private fun ExifInterface.readFocalLength(): String? {
    val raw = getAttribute(ExifInterface.TAG_FOCAL_LENGTH) ?: return null
    val value = if (raw.contains("/")) {
        val parts = raw.split("/")
        val num = parts.getOrNull(0)?.toDoubleOrNull()
        val den = parts.getOrNull(1)?.toDoubleOrNull()
        if (num != null && den != null && den != 0.0) num / den else null
    } else {
        raw.toDoubleOrNull()
    }
    return value?.let { "${it.toInt()} mm" }
}

private fun ExifInterface.readAperture(): String? {
    val f = getAttributeDouble(ExifInterface.TAG_F_NUMBER, -1.0)
    return if (f > 0) "\u0192${String.format(Locale.getDefault(), "%.2g", f)}" else null
}

private fun ExifInterface.readShutterSpeed(): String? {
    // getAttributeDouble handles the EXIF rational, getAttribute returns "num/den"
    val raw = getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, -1.0)
    if (raw <= 0) return null
    return if (raw < 1) "1/${Math.round(1 / raw)} s" else "${raw} s"
}

private fun ExifInterface.readGpsCoordinates(): String? {
    val latLong = latLong ?: return null
    return String.format(Locale.getDefault(), "%.5f, %.5f", latLong[0], latLong[1])
}

fun buildMetadata(
    exif: ExifInterface?,
    name: String,
    sizeBytes: Long,
    width: Int?,
    height: Int?,
    mimeType: String?,
    fallbackDateMillis: Long
): PhotoMetadata {
    val megapixels = if (width != null && height != null) {
        String.format(Locale.getDefault(), "%.1f MP", (width.toLong() * height.toLong()) / 1_000_000.0)
    } else null

    return PhotoMetadata(
        name = name,
        sizeBytes = sizeBytes,
        weekdayDateLabel = formatWeekdayDate(fallbackDateMillis),
        timeLabel = formatTime(fallbackDateMillis),
        dimensions = if (width != null && height != null) "$width \u00d7 $height" else null,
        megapixels = megapixels,
        format = mimeType?.substringAfterLast("/")?.uppercase(Locale.getDefault()),
        cameraMake = exif?.getAttribute(ExifInterface.TAG_MAKE),
        cameraModel = exif?.getAttribute(ExifInterface.TAG_MODEL),
        iso = exif?.readIso(),
        focalLength = exif?.readFocalLength(),
        aperture = exif?.readAperture(),
        shutterSpeed = exif?.readShutterSpeed(),
        gpsCoordinates = exif?.readGpsCoordinates()
    )
}

// Hidden files keep their EXIF through the byte copy
fun buildFileMetadata(file: File, isVideo: Boolean): PhotoMetadata {
    val exif = if (!isVideo) readExif(file) else null
    val dimensions = if (!isVideo) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth > 0 && options.outHeight > 0) options.outWidth to options.outHeight else null
    } else null

    val originalDateMillis = exif?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let { raw ->
        try {
            SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(raw)?.time
        } catch (e: Exception) {
            null
        }
    } ?: file.lastModified()

    return buildMetadata(
        exif = exif,
        name = file.name,
        sizeBytes = file.length(),
        width = dimensions?.first,
        height = dimensions?.second,
        mimeType = if (isVideo) "video/mp4" else "image/jpeg",
        fallbackDateMillis = originalDateMillis
    )
}
