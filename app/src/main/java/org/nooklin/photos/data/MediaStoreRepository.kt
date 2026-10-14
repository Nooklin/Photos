package org.nooklin.photos.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nooklin.photos.R
import org.nooklin.photos.appString

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val dateAdded: Long,
    val dateModified: Long,
    val isVideo: Boolean,
    val bucketId: String?,
    val bucketName: String?,
    val width: Int?,
    val height: Int?,
    val sizeBytes: Long?,
    val displayName: String?,
    val mimeType: String? = null,
    val captureFrameRate: Float? = null,
    val isFavorite: Boolean = false
) {
    // Heuristic, bucket names vary by OEM
    val isScreenshot: Boolean
        get() = bucketName?.contains("screenshot", ignoreCase = true) == true

    // Heuristic, naming varies by OEM
    val isScreenRecording: Boolean
        get() = isVideo && (
            bucketName?.contains("screen recording", ignoreCase = true) == true ||
                bucketName?.contains("screenrecord", ignoreCase = true) == true ||
                displayName?.contains("screenrecord", ignoreCase = true) == true
            )

    // Heuristic: aspect ratio
    val isPanorama: Boolean
        get() {
            val w = width ?: return false
            val h = height ?: return false
            if (w <= 0 || h <= 0) return false
            val ratio = maxOf(w, h).toFloat() / minOf(w, h).toFloat()
            return ratio > 2.5f
        }

    // Exact for RAW mime types, extension is the fallback
    val isRaw: Boolean
        get() {
            if (mimeType?.contains("dng", ignoreCase = true) == true ||
                mimeType?.contains("raw", ignoreCase = true) == true
            ) return true
            val name = displayName?.lowercase() ?: return false
            return listOf(".dng", ".raw", ".cr2", ".cr3", ".nef", ".arw", ".orf", ".rw2")
                .any { name.endsWith(it) }
        }

    // Capture rate from MediaStore, 90fps sits between normal (24-60) and slow-mo (120+)
    val isSlowMotion: Boolean
        get() = isVideo && (captureFrameRate ?: 0f) > 90f
}

class MediaStoreRepository(private val context: Context) {

    suspend fun queryMetadata(item: MediaItem): PhotoMetadata = withContext(Dispatchers.IO) {
        var displayName = item.uri.lastPathSegment ?: appString(R.string.nooklin_photos_app_unknown_2)
        var size = item.sizeBytes ?: 0L
        var width: Int? = item.width
        var height: Int? = item.height
        var mimeType: String? = null
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.MIME_TYPE
        )
        context.contentResolver.query(item.uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                if (nameCol >= 0) displayName = cursor.getStringOrNull(nameCol) ?: displayName
                if (sizeCol >= 0) size = cursor.getLong(sizeCol)
                if (widthCol >= 0 && !cursor.isNull(widthCol)) width = cursor.getInt(widthCol)
                if (heightCol >= 0 && !cursor.isNull(heightCol)) height = cursor.getInt(heightCol)
                if (mimeCol >= 0) mimeType = cursor.getStringOrNull(mimeCol)
            }
        }

        val exif = if (!item.isVideo) {
            readExif { context.contentResolver.openInputStream(item.uri) }
        } else null

        buildMetadata(
            exif = exif,
            name = displayName,
            sizeBytes = size,
            width = width,
            height = height,
            mimeType = mimeType,
            fallbackDateMillis = item.dateAdded * 1000L
        )
    }

    // Unused, kept for a possible save-as-copy
    suspend fun saveEditedCopy(bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        val displayName = ImageNaming.fileName(context, "jpg")
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val newUri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return@withContext null

        val succeeded = try {
            context.contentResolver.openOutputStream(newUri)?.use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
            } ?: false
        } catch (e: Exception) {
            false
        }

        if (!succeeded) {
            context.contentResolver.delete(newUri, null, null)
            return@withContext null
        }
        markNotPending(newUri)
        newUri
    }

    suspend fun duplicate(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                "copy_${System.currentTimeMillis()}_${item.displayName ?: "photo.jpg"}"
            )
            put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType ?: if (item.isVideo) "video/mp4" else "image/jpeg")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = if (item.isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val newUri = context.contentResolver.insert(collection, values) ?: return@withContext false
        val success = try {
            context.contentResolver.openOutputStream(newUri)?.use { output ->
                context.contentResolver.openInputStream(item.uri)?.use { input -> input.copyTo(output) }
            } != null
        } catch (e: Exception) {
            false
        }
        if (success) markNotPending(newUri) else context.contentResolver.delete(newUri, null, null)
        success
    }

    private fun markNotPending(uri: Uri) {
        context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
    }

    suspend fun queryMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        val collection = MediaStore.Files.getContentUri("external")
        val selection =
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection, MEDIA_PROJECTION, selection, selectionArgs, sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                items.add(cursor.toMediaItem())
            }
        }
        items
    }

    // Single lookup: recycle bin items are filtered out of loadAll
    suspend fun queryMediaById(id: Long): MediaItem? = withContext(Dispatchers.IO) {
        val collection = MediaStore.Files.getContentUri("external")
        val selection = "${MediaStore.Files.FileColumns._ID} = ?"
        val selectionArgs = arrayOf(id.toString())

        context.contentResolver.query(
            collection, MEDIA_PROJECTION, selection, selectionArgs, null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@withContext null
            cursor.toMediaItem()
        }
    }
}

private val MEDIA_PROJECTION = arrayOf(
    MediaStore.Files.FileColumns._ID,
    MediaStore.Files.FileColumns.DATE_ADDED,
    MediaStore.Files.FileColumns.DATE_MODIFIED,
    MediaStore.Files.FileColumns.MEDIA_TYPE,
    MediaStore.Files.FileColumns.BUCKET_ID,
    MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
    MediaStore.Files.FileColumns.WIDTH,
    MediaStore.Files.FileColumns.HEIGHT,
    MediaStore.Files.FileColumns.ORIENTATION,
    MediaStore.Files.FileColumns.SIZE,
    MediaStore.Files.FileColumns.DISPLAY_NAME,
    MediaStore.Files.FileColumns.MIME_TYPE,
    MediaStore.Files.FileColumns.CAPTURE_FRAMERATE
)

private fun Cursor.toMediaItem(): MediaItem {
    val idCol = getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
    val dateCol = getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
    val dateModifiedCol = getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
    val typeCol = getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
    val bucketIdCol = getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
    val bucketNameCol = getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
    val widthCol = getColumnIndex(MediaStore.Files.FileColumns.WIDTH)
    val heightCol = getColumnIndex(MediaStore.Files.FileColumns.HEIGHT)
    val orientationCol = getColumnIndex(MediaStore.Files.FileColumns.ORIENTATION)
    val sizeCol = getColumnIndex(MediaStore.Files.FileColumns.SIZE)
    val displayNameCol = getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
    val mimeCol = getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
    val frameRateCol = getColumnIndex(MediaStore.Files.FileColumns.CAPTURE_FRAMERATE)

    val id = getLong(idCol)
    val isVideo = getInt(typeCol) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
    var width = if (widthCol >= 0 && !isNull(widthCol)) getInt(widthCol) else null
    var height = if (heightCol >= 0 && !isNull(heightCol)) getInt(heightCol) else null
    // WIDTH/HEIGHT can be pre-rotation, swap them for the real on-screen ratio
    val orientationDegrees = if (orientationCol >= 0 && !isNull(orientationCol)) getInt(orientationCol) else 0
    if ((orientationDegrees == 90 || orientationDegrees == 270) && width != null && height != null) {
        val swap = width; width = height; height = swap
    }
    val sizeBytes = if (sizeCol >= 0 && !isNull(sizeCol)) getLong(sizeCol) else null
    val displayName = if (displayNameCol >= 0) getStringOrNull(displayNameCol) else null
    val mimeType = if (mimeCol >= 0) getStringOrNull(mimeCol) else null
    val captureFrameRate = if (frameRateCol >= 0 && !isNull(frameRateCol)) getFloat(frameRateCol) else null
    val contentUri = if (isVideo) {
        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
    } else {
        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
    }
    return MediaItem(
        id, contentUri, getLong(dateCol), getLong(dateModifiedCol), isVideo,
        getStringOrNull(bucketIdCol), getStringOrNull(bucketNameCol),
        width, height, sizeBytes, displayName, mimeType, captureFrameRate
    )
}

private fun Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)
