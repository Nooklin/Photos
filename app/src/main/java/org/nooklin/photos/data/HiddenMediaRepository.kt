package org.nooklin.photos.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nooklin.photos.data.local.HiddenAlbumEntity
import org.nooklin.photos.data.local.HiddenAlbumMembershipEntity
import org.nooklin.photos.data.local.HiddenDatabase
import org.nooklin.photos.data.local.HiddenMediaEntity
import org.nooklin.photos.data.local.LocalMetadataStore
import org.nooklin.photos.data.local.NooklinDatabase
import java.io.File
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import android.media.ThumbnailUtils
import android.util.Size
import android.graphics.ImageDecoder

// thumbnailFile falls back to file when there is no thumbnail
data class HiddenItem(
    val file: File,
    val thumbnailFile: File,
    val isVideo: Boolean,
    val hiddenAt: Long,
    val isFavorite: Boolean,
    val isScreenshot: Boolean = false,
    val isSlowMotion: Boolean = false,
    val isRaw: Boolean = false,
    val isPanorama: Boolean = false,
    val isScreenRecording: Boolean = false,
    val width: Int? = null,
    val height: Int? = null,
    val mimeType: String? = null,
    val deletedAt: Long? = null
)

data class HiddenAlbumWithPreview(
    val id: Long,
    val name: String,
    val itemCount: Int,
    val previewFiles: List<File>
)

sealed class UnhideResult {
    data class Success(val uri: Uri, val width: Int? = null, val height: Int? = null) : UnhideResult()
    object Failed : UnhideResult()
    object NotFound : UnhideResult()
}

// Files live in filesDir: private, never scanned by MediaStore
class HiddenMediaRepository(
    private val context: Context,
    // Only the recycle bin lives here
    private val localMetadataStore: LocalMetadataStore
) {
    private val hiddenDao = HiddenDatabase.getInstance(context).hiddenDao()
    private val libraryDb = NooklinDatabase.getInstance(context)

    // Editor originals (for Revert) follow the photo through hide and unhide
    private val originals = OriginalsStore(context)

    private val hiddenDir: File
        get() = File(context.filesDir, "hidden").apply { mkdirs() }

    private val thumbsDir: File
        get() = File(context.filesDir, "hidden_thumbs").apply { mkdirs() }

    private fun generateVideoThumbnail(videoFile: File, destination: File): Boolean = try {
        val bitmap = ThumbnailUtils.createVideoThumbnail(videoFile, Size(512, 512), null)
        destination.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        true
    } catch (e: Exception) {
        destination.delete()
        false
    }

    suspend fun backfillVideoThumbnails(): Boolean = withContext(Dispatchers.IO) {
        var created = false
        for (entry in hiddenDao.allMedia()) {
            if (!entry.isVideo) continue
            val video = File(hiddenDir, entry.fileName)
            val thumbnail = File(thumbsDir, entry.fileName)
            if (video.exists() && !thumbnail.exists() && generateVideoThumbnail(video, thumbnail)) created = true
        }
        created
    }

    // HEIC needs ImageDecoder
    private fun decodeThumbnailWithImageDecoder(sourceUri: Uri, targetSize: Int): Bitmap? = try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, sourceUri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > targetSize * 2) {
                val scale = targetSize * 2f / longest
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1)
                )
            }
        }
    } catch (e: Exception) {
        null
    }

    private fun generatePhotoThumbnail(sourceUri: Uri, destination: File) {
        try {
            val targetSize = 512
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOptions)
            }
            var sampleSize = 1
            while (
                boundsOptions.outWidth / sampleSize > targetSize * 2 ||
                boundsOptions.outHeight / sampleSize > targetSize * 2
            ) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            var alreadyUpright = false
            val decoded: Bitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: decodeThumbnailWithImageDecoder(sourceUri, targetSize)?.also { alreadyUpright = true } ?: return

            // decodeStream ignores EXIF orientation, thumbnails came out sideways
            val rotationDegrees = if (alreadyUpright) 0 else readExif { context.contentResolver.openInputStream(sourceUri) }
                ?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                ?.let {
                    when (it) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                } ?: 0

            val bitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                if (rotated !== decoded) decoded.recycle()
                rotated
            } else {
                decoded
            }

            destination.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            }
            bitmap.recycle()
        } catch (e: Exception) {
        }
    }

    // Null if the copy failed, so the original is never deleted without a backup
    suspend fun copyToHidden(item: MediaItem): File? = withContext(Dispatchers.IO) {
        val mimeType = item.mimeType ?: if (item.isVideo) "video/mp4" else "image/jpeg"
        val extension = fileExtensionFor(mimeType, item.isVideo)
        val destination = File(hiddenDir, "${item.id}_${System.currentTimeMillis()}.$extension")
        val copied = try {
            context.contentResolver.openInputStream(item.uri)?.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            } != null
        } catch (e: Exception) {
            false
        }
        if (!copied || destination.length() <= 0L) {
            destination.delete()
            return@withContext null
        }
        if (item.isVideo) {
            generateVideoThumbnail(destination, File(thumbsDir, destination.name))
        } else {
            generatePhotoThumbnail(item.uri, File(thumbsDir, destination.name))
        }
        // Copied, not moved: if the delete prompt is declined the photo keeps its Revert
        originals.copy(OriginalsStore.mediaKey(item.id), OriginalsStore.hiddenKey(destination.name))
        hiddenDao.insertMedia(
            HiddenMediaEntity(
                fileName = destination.name,
                isVideo = item.isVideo,
                hiddenAt = System.currentTimeMillis(),
                isFavorite = false,
                isScreenshot = item.isScreenshot,
                isSlowMotion = item.isSlowMotion,
                isRaw = item.isRaw,
                isPanorama = item.isPanorama,
                isScreenRecording = item.isScreenRecording,
                width = item.width,
                height = item.height,
                mimeType = mimeType,
                dateAdded = item.dateAdded
            )
        )
        destination
    }

    // After the delete is accepted nothing about the item stays in NooklinDatabase
    suspend fun purgeLibraryData(mediaIds: List<Long>) = withContext(Dispatchers.IO) {
        if (mediaIds.isEmpty()) return@withContext
        libraryDb.albumDao().deleteMembershipsForMediaIds(mediaIds)
        libraryDb.petDao().deleteMembershipsForMediaIds(mediaIds)
        libraryDb.photoAnalysisDao().deleteTagsForMediaIds(mediaIds)
        libraryDb.photoAnalysisDao().clearStatusForMediaIds(mediaIds)
        libraryDb.photoAnalysisDao().deleteHashesForMediaIds(mediaIds)
        libraryDb.personDao().deleteFacesForMediaIds(mediaIds)
    }

    suspend fun discardCopy(fileName: String) = withContext(Dispatchers.IO) {
        File(hiddenDir, fileName).delete()
        File(thumbsDir, fileName).delete()
        originals.delete(OriginalsStore.hiddenKey(fileName))
        hiddenDao.deleteMedia(fileName)
    }

    private val thumbnailSizes = HashMap<String, Pair<Int, Int>>()

    private fun thumbnailDimensions(thumbnail: File): Pair<Int, Int>? {
        val key = thumbnail.name + "|" + thumbnail.lastModified()
        synchronized(thumbnailSizes) { thumbnailSizes[key]?.let { return it } }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(thumbnail.absolutePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        val size = options.outWidth to options.outHeight
        synchronized(thumbnailSizes) { thumbnailSizes[key] = size }
        return size
    }

    private suspend fun listEntries(deleted: Boolean): List<HiddenItem> = withContext(Dispatchers.IO) {
        hiddenDao.allMedia().filter { (it.deletedAt != null) == deleted }.mapNotNull { entry ->
            val file = File(hiddenDir, entry.fileName)
            if (file.exists()) {
                val thumb = File(thumbsDir, entry.fileName)
                val thumbnailFile = if (thumb.exists()) thumb else file
                // Thumbnail is upright, the stored size can be unrotated
                val thumbnailSize = if (thumb.exists()) thumbnailDimensions(thumb) else null
                HiddenItem(
                    file = file,
                    thumbnailFile = thumbnailFile,
                    isVideo = entry.isVideo,
                    hiddenAt = entry.hiddenAt,
                    isFavorite = entry.isFavorite,
                    isScreenshot = entry.isScreenshot,
                    isSlowMotion = entry.isSlowMotion,
                    isRaw = entry.isRaw,
                    isPanorama = entry.isPanorama,
                    isScreenRecording = entry.isScreenRecording,
                    width = thumbnailSize?.first ?: entry.width,
                    height = thumbnailSize?.second ?: entry.height,
                    mimeType = entry.mimeType,
                    deletedAt = entry.deletedAt
                )
            } else null
        }
            .sortedByDescending { it.hiddenAt }
    }

    suspend fun listHidden(): List<HiddenItem> = listEntries(deleted = false)

    // Items in the Hidden Recently Deleted, still private files in the app
    suspend fun listDeleted(): List<HiddenItem> = listEntries(deleted = true)

    // Wipes deleted items older than 30 days, no system dialog needed because the files are the app's own
    suspend fun purgeExpired() = withContext(Dispatchers.IO) {
        val limit = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        hiddenDao.allMedia()
            .filter { val d = it.deletedAt; d != null && d < limit }
            .forEach { discardCopy(it.fileName) }
    }

    suspend fun purge(fileNames: List<String>) {
        fileNames.forEach { discardCopy(it) }
    }

    suspend fun restoreDeleted(fileName: String) = withContext(Dispatchers.IO) {
        hiddenDao.setDeletedAt(fileName, null)
    }

    suspend fun updateSize(fileName: String, width: Int, height: Int) {
        hiddenDao.setSize(fileName, width, height)
        // An edit or revert can change the format (HEIC to JPEG), so reread the type
        val actualType = withContext(Dispatchers.IO) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(File(hiddenDir, fileName).absolutePath, options)
            options.outMimeType
        }
        if (actualType != null) hiddenDao.setMimeType(fileName, actualType)
    }

    suspend fun toggleFavorite(fileName: String, favorite: Boolean) {
        hiddenDao.setFavorite(fileName, favorite)
    }

    suspend fun createAlbum(name: String): Long = hiddenDao.insertAlbum(HiddenAlbumEntity(name = name))

    suspend fun renameAlbum(albumId: Long, newName: String) {
        hiddenDao.renameAlbum(albumId, newName)
    }

    suspend fun deleteAlbum(albumId: Long) {
        hiddenDao.deleteAlbum(albumId)
    }

    suspend fun addToAlbum(albumId: Long, fileName: String) {
        hiddenDao.addMembership(HiddenAlbumMembershipEntity(albumId, fileName))
    }

    suspend fun removeFromAlbum(albumId: Long, fileName: String) {
        hiddenDao.removeMembership(albumId, fileName)
    }

    suspend fun listAlbumsWithPreview(items: List<HiddenItem>): List<HiddenAlbumWithPreview> = withContext(Dispatchers.IO) {
        val albums = hiddenDao.allAlbums()
        val membership = hiddenDao.allMemberships()
        val membershipByAlbum = membership.groupBy({ it.albumId }, { it.fileName })
        albums.map { album ->
            val memberNames = membershipByAlbum[album.id].orEmpty().toSet()
            // Filter the sorted items so the first 4 are the newest
            val files = items.filter { it.file.name in memberNames }.map { it.thumbnailFile }
            HiddenAlbumWithPreview(album.id, album.name, files.size, files.take(4))
        }
    }

    suspend fun albumMembershipSnapshot(): List<Pair<Long, String>> = withContext(Dispatchers.IO) {
        hiddenDao.allMemberships().map { it.albumId to it.fileName }
    }

    // Backup zip, files are wiped on uninstall
    suspend fun exportFlatZip(
        context: Context,
        destination: Uri,
        onProgress: (done: Int, total: Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val items = listHidden()
            context.contentResolver.openOutputStream(destination)?.use { out ->
                ZipOutputStream(out).use { zip ->
                    // Already compressed, deflate only costs CPU
                    zip.setLevel(Deflater.NO_COMPRESSION)
                    items.forEachIndexed { index, item ->
                        writeFileToZip(zip, item.file, item.file.name)
                        onProgress(index + 1, items.size)
                    }
                }
            } ?: return@withContext deleteDestination(context, destination)
            true
        } catch (e: Exception) {
            deleteDestination(context, destination)
        }
    }

    suspend fun exportSelectedZip(
        context: Context,
        destination: Uri,
        fileNames: Set<String>,
        onProgress: (done: Int, total: Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val items = listHidden().filter { it.file.name in fileNames }
            context.contentResolver.openOutputStream(destination)?.use { out ->
                ZipOutputStream(out).use { zip ->
                    zip.setLevel(Deflater.NO_COMPRESSION)
                    items.forEachIndexed { index, item ->
                        writeFileToZip(zip, item.file, item.file.name)
                        onProgress(index + 1, items.size)
                    }
                }
            } ?: return@withContext deleteDestination(context, destination)
            true
        } catch (e: Exception) {
            deleteDestination(context, destination)
        }
    }

    // A photo in several albums is written once per album
    suspend fun exportOrganizedZip(
        context: Context,
        destination: Uri,
        onProgress: (done: Int, total: Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val items = listHidden()
            val filesByName = items.associateBy { it.file.name }
            val albums = hiddenDao.allAlbums()
            val membership = hiddenDao.allMemberships()
            val membershipByAlbum = membership.groupBy({ it.albumId }, { it.fileName })
            val namesInAnyAlbum = membership.map { it.fileName }.toSet()
            val loose = items.filter { it.file.name !in namesInAnyAlbum }

            // Count writes, not items, or progress overshoots
            val albumWriteCount = albums.sumOf { album ->
                membershipByAlbum[album.id].orEmpty().count { it in filesByName }
            }
            val total = loose.size + albumWriteCount
            var done = 0

            val usedFolderNames = mutableSetOf<String>()
            fun uniqueFolderName(album: HiddenAlbumEntity): String {
                val base = album.name.replace(Regex("[/\\\\:*?\"<>|]"), "_").ifBlank { "Untitled" }
                val candidate = if (usedFolderNames.add(base)) base else "${base}_${album.id}".also { usedFolderNames.add(it) }
                return candidate
            }

            context.contentResolver.openOutputStream(destination)?.use { out ->
                ZipOutputStream(out).use { zip ->
                    zip.setLevel(Deflater.NO_COMPRESSION)
                    loose.forEach { item ->
                        writeFileToZip(zip, item.file, "Hidden Images/${item.file.name}")
                        onProgress(++done, total)
                    }
                    albums.forEach { album ->
                        val folder = uniqueFolderName(album)
                        membershipByAlbum[album.id].orEmpty().forEach { fileName ->
                            val file = filesByName[fileName]?.file ?: return@forEach
                            writeFileToZip(zip, file, "Hidden Albums/$folder/$fileName")
                            onProgress(++done, total)
                        }
                    }
                }
            } ?: return@withContext deleteDestination(context, destination)
            true
        } catch (e: Exception) {
            deleteDestination(context, destination)
        }
    }

    // No partial zip left behind
    private fun deleteDestination(context: Context, destination: Uri): Boolean {
        try {
            DocumentsContract.deleteDocument(context.contentResolver, destination)
        } catch (e: Exception) {
            // Not every provider can delete
        }
        return false
    }

    private fun writeFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(zip, 64 * 1024) }
        zip.closeEntry()
    }

    private suspend fun reinsertIntoMediaStore(fileName: String): UnhideResult =
        withContext(Dispatchers.IO) {
            val entry = hiddenDao.mediaFor(fileName)
                ?: return@withContext UnhideResult.NotFound
            val file = File(hiddenDir, fileName)
            if (!file.exists()) return@withContext UnhideResult.NotFound

            val resolver = context.contentResolver
            val mimeType = entry.mimeType ?: if (entry.isVideo) "video/mp4" else "image/jpeg"
            // An edit can change the format, so the extension follows the type
            val displayName = if (entry.isVideo) {
                fileName.substringBeforeLast('.') + "." + fileExtensionFor(mimeType, true)
            } else {
                ImageNaming.fileName(context, fileExtensionFor(mimeType, false))
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = if (entry.isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val newUri = resolver.insert(collection, values) ?: return@withContext UnhideResult.Failed

            // Only delete the hidden copy after a confirmed write
            val copySucceeded = try {
                val output = resolver.openOutputStream(newUri)
                if (output == null) {
                    false
                } else {
                    output.use { out -> file.inputStream().use { input -> input.copyTo(out) } }
                    true
                }
            } catch (e: Exception) {
                false
            }

            if (!copySucceeded) {
                resolver.delete(newUri, null, null)
                return@withContext UnhideResult.Failed
            }

            resolver.update(newUri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)

            // New library id: carry the stored original over
            originals.copy(OriginalsStore.hiddenKey(fileName), OriginalsStore.mediaKey(ContentUris.parseId(newUri)))
            originals.delete(OriginalsStore.hiddenKey(fileName))
            // MediaStore stamps the new row with today, so the old library date goes back as a sort date
            entry.dateAdded?.let { localMetadataStore.setSortDate(ContentUris.parseId(newUri), it) }
            file.delete()
            File(thumbsDir, fileName).delete()
            hiddenDao.deleteMedia(fileName)
            UnhideResult.Success(newUri, entry.width, entry.height)
        }

    suspend fun unhide(fileName: String): UnhideResult = reinsertIntoMediaStore(fileName)

    // Stays a private file in the app, only marked as deleted
    suspend fun trash(fileName: String) = withContext(Dispatchers.IO) {
        hiddenDao.setDeletedAt(fileName, System.currentTimeMillis())
    }
}
