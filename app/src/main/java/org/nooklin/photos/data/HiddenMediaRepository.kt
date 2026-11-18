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
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.util.Size
import android.graphics.ImageDecoder

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
    val deletedAt: Long? = null,
    val originalName: String? = null,
    val originalModified: Long? = null,
    val originalTaken: Long? = null
) {
    val sortDate: Long get() = originalTaken ?: originalModified ?: hiddenAt
}

data class HiddenAlbumWithPreview(
    val id: Long,
    val name: String,
    val itemCount: Int,
    val previewFiles: List<File>,
    val parentId: Long? = null,
    val folderCount: Int = 0,
    val coverFileName: String? = null
)

sealed class UnhideResult {
    data class Success(val uri: Uri, val width: Int? = null, val height: Int? = null) : UnhideResult()
    object Failed : UnhideResult()
    object NotFound : UnhideResult()
}

// Files live in filesDir: private, never scanned by MediaStore
class HiddenMediaRepository(
    private val context: Context,
    private val localMetadataStore: LocalMetadataStore
) {
    internal val hiddenDao = HiddenDatabase.getInstance(context).hiddenDao()
    private val libraryDb = NooklinDatabase.getInstance(context)

    private val originals = OriginalsStore(context)

    internal val hiddenDir: File
        get() = File(context.filesDir, "hidden").apply { mkdirs() }

    internal val thumbsDir: File
        get() = File(context.filesDir, "hidden_thumbs").apply { mkdirs() }

    private fun brightness(bitmap: Bitmap): Int {
        val small = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
        var sum = 0L
        for (y in 0 until 16) for (x in 0 until 16) {
            val px = small.getPixel(x, y)
            sum += (((px shr 16) and 0xFF) * 299 + ((px shr 8) and 0xFF) * 587 + (px and 0xFF) * 114) / 1000
        }
        if (small !== bitmap) small.recycle()
        return (sum / 256).toInt()
    }

    // first frame is often black (fade in), so try a few spots and keep the first bright one
    internal fun generateVideoThumbnail(videoFile: File, destination: File): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val spots = listOf(0.5, 0.25, 0.1, 0.75, 0.9, 0.0).map { (durationMs * 1000 * it).toLong() }
            var best: Bitmap? = null
            var bestLuma = -1
            for (timeUs in spots) {
                val frame = retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 512, 512) ?: continue
                val luma = brightness(frame)
                if (luma > bestLuma) {
                    best?.recycle()
                    best = frame
                    bestLuma = luma
                } else {
                    frame.recycle()
                }
                if (bestLuma >= 30) break
            }
            val result = best ?: return false
            destination.outputStream().use { result.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            result.recycle()
            true
        } catch (e: Exception) {
            destination.delete()
            false
        } finally {
            retriever.release()
        }
    }

    private fun isDark(thumbnail: File): Boolean {
        val bitmap = BitmapFactory.decodeFile(thumbnail.absolutePath) ?: return false
        val dark = brightness(bitmap) < 30
        bitmap.recycle()
        return dark
    }

    // same date the viewer shows: exif capture time, else the file date
    suspend fun backfillTakenDates() = withContext(Dispatchers.IO) {
        for (entry in hiddenDao.allMedia()) {
            if (entry.originalTaken != null) continue
            val file = File(hiddenDir, entry.fileName)
            if (!file.exists()) continue
            val exifDate = if (entry.isVideo) null else readExif(file)
                ?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let {
                    try {
                        SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(it)?.time
                    } catch (e: Exception) {
                        null
                    }
                }
            hiddenDao.setTaken(entry.fileName, exifDate ?: file.lastModified())
        }
    }

    suspend fun backfillVideoThumbnails(): Boolean = withContext(Dispatchers.IO) {
        var created = false
        val marker = File(context.filesDir, "hidden_thumbs_bright")
        val fixDark = !marker.exists()
        for (entry in hiddenDao.allMedia()) {
            if (!entry.isVideo) continue
            val video = File(hiddenDir, entry.fileName)
            val thumbnail = File(thumbsDir, entry.fileName)
            if (!video.exists()) continue
            val missing = !thumbnail.exists()
            if ((missing || (fixDark && isDark(thumbnail))) && generateVideoThumbnail(video, thumbnail)) created = true
        }
        if (fixDark) marker.createNewFile()
        created
    }

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

    internal fun generatePhotoThumbnail(sourceUri: Uri, destination: File) {
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
        if (item.dateModified > 0L) destination.setLastModified(item.dateModified * 1000)
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
                dateAdded = item.dateAdded,
                originalName = item.displayName,
                originalModified = item.dateModified.takeIf { it > 0L }?.times(1000),
                originalTaken = item.dateTaken
            )
        )
        destination
    }

    // after the delete is accepted nothing about the item stays in NooklinDatabase
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
                    deletedAt = entry.deletedAt,
                    originalName = entry.originalName,
                    originalModified = entry.originalModified,
                    originalTaken = entry.originalTaken
                )
            } else null
        }
            .sortedByDescending { it.sortDate }
    }

    suspend fun listHidden(): List<HiddenItem> = listEntries(deleted = false)

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
        val actualType = withContext(Dispatchers.IO) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(File(hiddenDir, fileName).absolutePath, options)
            options.outMimeType
        }
        if (actualType != null) hiddenDao.setMimeType(fileName, actualType)
    }

    // the edit is a new file next to the original, in the same albums
    suspend fun registerEditedCopy(sourceName: String, newName: String, width: Int, height: Int) = withContext(Dispatchers.IO) {
        val entry = hiddenDao.allMedia().find { it.fileName == sourceName } ?: return@withContext
        val source = File(hiddenDir, sourceName)
        val file = File(hiddenDir, newName)
        if (!file.exists()) return@withContext
        file.setLastModified(source.lastModified())
        val original = entry.originalName
        val editedName = if (original != null) {
            original.substringBeforeLast('.', original) + " (edited)." + file.extension
        } else null
        hiddenDao.insertMedia(
            entry.copy(
                fileName = newName,
                hiddenAt = System.currentTimeMillis(),
                isFavorite = false,
                deletedAt = null,
                width = width,
                height = height,
                mimeType = mediaKind(newName, null) ?: entry.mimeType,
                originalName = editedName
            )
        )
        hiddenDao.allMemberships().filter { it.fileName == sourceName }.forEach {
            hiddenDao.addMembership(HiddenAlbumMembershipEntity(it.albumId, newName))
        }
    }

    suspend fun duplicate(fileName: String): String? = withContext(Dispatchers.IO) {
        val entry = hiddenDao.allMedia().find { it.fileName == fileName } ?: return@withContext null
        val source = File(hiddenDir, fileName)
        if (!source.exists()) return@withContext null
        val destination = File(hiddenDir, "copy_${System.currentTimeMillis()}_$fileName")
        val copied = try {
            source.copyTo(destination)
            true
        } catch (e: Exception) {
            false
        }
        if (!copied) {
            destination.delete()
            return@withContext null
        }
        destination.setLastModified(source.lastModified())
        val sourceThumb = File(thumbsDir, fileName)
        if (sourceThumb.exists()) {
            try {
                sourceThumb.copyTo(File(thumbsDir, destination.name))
            } catch (e: Exception) {
            }
        }
        hiddenDao.insertMedia(
            entry.copy(
                fileName = destination.name,
                hiddenAt = System.currentTimeMillis(),
                isFavorite = false,
                deletedAt = null,
                originalName = "copy_${System.currentTimeMillis()}_${entry.originalName ?: fileName}"
            )
        )
        destination.name
    }

    suspend fun toggleFavorite(fileName: String, favorite: Boolean) {
        hiddenDao.setFavorite(fileName, favorite)
    }

    suspend fun createAlbum(name: String): Long = hiddenDao.insertAlbum(HiddenAlbumEntity(name = name))

    suspend fun createFolder(parentId: Long, name: String): Long =
        hiddenDao.insertAlbum(HiddenAlbumEntity(name = name, parentId = parentId))

    suspend fun setAlbumCover(albumId: Long, fileName: String?) {
        hiddenDao.setCover(albumId, fileName)
    }

    suspend fun renameAlbum(albumId: Long, newName: String) {
        hiddenDao.renameAlbum(albumId, newName)
    }

    suspend fun deleteAlbum(albumId: Long) {
        hiddenDao.deleteAlbum(albumId)
    }

    suspend fun mergeAlbums(sourceAlbumId: Long, targetAlbumId: Long) {
        hiddenDao.mergeAlbums(sourceAlbumId, targetAlbumId)
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
        val folderCounts = albums.mapNotNull { it.parentId }.groupingBy { it }.eachCount()
        val foldersByParent = albums.filter { it.parentId != null }.groupBy { it.parentId }
        fun membersOf(albumId: Long): List<HiddenItem> {
            val names = membershipByAlbum[albumId].orEmpty().toSet()
            // Filter the sorted items so the first 4 are the newest
            return items.filter { it.file.name in names }
        }
        fun treeMembers(albumId: Long): List<HiddenItem> =
            (membersOf(albumId) + foldersByParent[albumId].orEmpty().flatMap { treeMembers(it.id) }).distinctBy { it.file.name }
        albums.map { album ->
            val members = membersOf(album.id)
            val cover = members.find { it.file.name == album.coverFileName }
            val folders = foldersByParent[album.id].orEmpty()
            val folderFronts = folders.mapNotNull { folder ->
                val inFolder = treeMembers(folder.id)
                (inFolder.find { it.file.name == folder.coverFileName } ?: inFolder.firstOrNull())?.thumbnailFile
            }.take(4)
            val total = treeMembers(album.id).size
            val files = when {
                folderFronts.isNotEmpty() -> folderFronts
                cover != null -> (listOf(cover) + members.filter { it !== cover }).map { it.thumbnailFile }
                else -> members.map { it.thumbnailFile }
            }
            HiddenAlbumWithPreview(album.id, album.name, total, files.take(4), album.parentId, folders.size, album.coverFileName)
        }
    }

    suspend fun albumMembershipSnapshot(): List<Pair<Long, String>> = withContext(Dispatchers.IO) {
        hiddenDao.allMemberships().map { it.albumId to it.fileName }
    }

    // Entry names in a zip: the original name, only characters a zip cannot hold are replaced, " (2)" for the same name in one directory
    private class EntryNamer {
        private val used = HashSet<String>()
        fun entry(dir: String, item: HiddenItem): String {
            val name = (item.originalName ?: item.file.name).replace(Regex("[/\\\\:*?\"<>|]"), "_")
            val prefix = if (dir.isEmpty()) "" else "$dir/"
            var candidate = prefix + name
            var n = 2
            val base = name.substringBeforeLast('.', name)
            val ext = name.substringAfterLast('.', "")
            while (!used.add(candidate)) {
                candidate = "$prefix$base ($n)" + if (ext.isNotEmpty()) ".$ext" else ""
                n++
            }
            return candidate
        }
    }

    private fun writeHiddenZip(
        zip: ZipOutputStream,
        writes: List<Pair<String, HiddenItem>>,
        onProgress: (done: Int, total: Int) -> Unit
    ) {
        zip.setLevel(Deflater.NO_COMPRESSION)
        val files = JSONObject()
        writes.forEach { (entryName, item) ->
            files.put(entryName, JSONObject().apply {
                item.originalName?.let { put("name", it) }
                item.originalModified?.let { put("modified", it) }
                item.originalTaken?.let { put("taken", it) }
            })
        }
        zip.putNextEntry(ZipEntry(METADATA_FILE))
        zip.write(JSONObject().put("version", 1).put("files", files).toString().toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        writes.forEachIndexed { index, (entryName, item) ->
            writeFileToZip(zip, item, entryName)
            onProgress(index + 1, writes.size)
        }
    }

    suspend fun exportFlatZip(
        context: Context,
        destination: Uri,
        onProgress: (done: Int, total: Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val namer = EntryNamer()
            val writes = listHidden().map { namer.entry("", it) to it }
            context.contentResolver.openOutputStream(destination)?.use { out ->
                ZipOutputStream(out).use { zip -> writeHiddenZip(zip, writes, onProgress) }
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
            val namer = EntryNamer()
            val writes = listHidden().filter { it.file.name in fileNames }.map { namer.entry("", it) to it }
            context.contentResolver.openOutputStream(destination)?.use { out ->
                ZipOutputStream(out).use { zip -> writeHiddenZip(zip, writes, onProgress) }
            } ?: return@withContext deleteDestination(context, destination)
            true
        } catch (e: Exception) {
            deleteDestination(context, destination)
        }
    }

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

            val usedFolderNames = mutableSetOf<String>()
            val albumsById = albums.associateBy { it.id }
            val folderNames = mutableMapOf<Long, String>()
            fun uniqueFolderName(album: HiddenAlbumEntity): String {
                folderNames[album.id]?.let { return it }
                val clean = album.name.replace(Regex("[/\\\\:*?\"<>|]"), "_").ifBlank { "Untitled" }
                val parent = album.parentId?.let { albumsById[it] }
                val base = if (parent != null) uniqueFolderName(parent) + "/" + clean else clean
                val candidate = if (usedFolderNames.add(base)) base else "${base}_${album.id}".also { usedFolderNames.add(it) }
                folderNames[album.id] = candidate
                return candidate
            }

            // Decided first so progress counts writes, not items
            val namer = EntryNamer()
            val writes = mutableListOf<Pair<String, HiddenItem>>()
            loose.forEach { writes.add(namer.entry("Hidden Images", it) to it) }
            albums.forEach { album ->
                val folder = uniqueFolderName(album)
                membershipByAlbum[album.id].orEmpty().forEach { fileName ->
                    val item = filesByName[fileName] ?: return@forEach
                    writes.add(namer.entry("Hidden Albums/$folder", item) to item)
                }
            }

            context.contentResolver.openOutputStream(destination)?.use { out ->
                ZipOutputStream(out).use { zip -> writeHiddenZip(zip, writes, onProgress) }
            } ?: return@withContext deleteDestination(context, destination)
            true
        } catch (e: Exception) {
            deleteDestination(context, destination)
        }
    }

    private fun deleteDestination(context: Context, destination: Uri): Boolean {
        try {
            DocumentsContract.deleteDocument(context.contentResolver, destination)
        } catch (e: Exception) {
            // not every provider can delete
        }
        return false
    }

    private fun writeFileToZip(zip: ZipOutputStream, item: HiddenItem, entryName: String) {
        val entry = ZipEntry(entryName)
        entry.time = item.originalModified ?: item.file.lastModified()
        zip.putNextEntry(entry)
        item.file.inputStream().use { it.copyTo(zip, 64 * 1024) }
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
            // an edit can change the format, so the extension follows the type
            val original = entry.originalName
            val displayName = if (original != null) {
                // the original name stays. Only when the format changed (an edit) the extension follows the new type
                val kind = mediaKind(original, null)
                if (kind == null || kind == mimeType.lowercase()) {
                    original
                } else {
                    original.substringBeforeLast('.', original) + "." + fileExtensionFor(mimeType, entry.isVideo)
                }
            } else if (entry.isVideo) {
                fileName.substringBeforeLast('.') + "." + fileExtensionFor(mimeType, true)
            } else {
                ImageNaming.fileName(context, fileExtensionFor(mimeType, false))
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                (entry.originalTaken ?: entry.originalModified)?.let { put(MediaStore.MediaColumns.DATE_TAKEN, it) }
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

            resolver.update(
                newUri,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                    entry.originalModified?.let { put(MediaStore.MediaColumns.DATE_MODIFIED, it / 1000) }
                },
                null,
                null
            )

            originals.copy(OriginalsStore.hiddenKey(fileName), OriginalsStore.mediaKey(ContentUris.parseId(newUri)))
            originals.delete(OriginalsStore.hiddenKey(fileName))
            // MediaStore stamps the new row with today, so the old library date goes back as a sort date
            if (entry.originalTaken == null) entry.dateAdded?.let { localMetadataStore.setSortDate(ContentUris.parseId(newUri), it) }
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
