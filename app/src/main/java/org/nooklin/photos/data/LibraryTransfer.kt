package org.nooklin.photos.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.nooklin.photos.data.local.AlbumEntity
import org.nooklin.photos.data.local.LocalMetadataStore
import org.nooklin.photos.data.local.NooklinDatabase

// Backup and import for the normal library: the same ideas as the Hidden Section, on MediaStore and the album database.

private fun sha256Of(file: java.io.InputStream): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(64 * 1024)
    while (true) {
        val n = file.read(buffer)
        if (n < 0) break
        digest.update(buffer, 0, n)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

// Into the library: the file becomes a new MediaStore item and the album gets its id
class LibraryImportSink(private val context: Context) : ImportSink {
    private val dao = NooklinDatabase.getInstance(context).albumDao()
    private val store = MediaStoreRepository(context)
    private val metadata = LocalMetadataStore(context)
    private var albums: MutableList<AlbumEntity>? = null

    private suspend fun albumList(): MutableList<AlbumEntity> =
        albums ?: dao.observeAlbums().first().toMutableList().also { albums = it }

    override suspend fun topLevelAlbumNames(): Set<String> =
        dao.observeAlbums().first().filter { it.parentId == null }.map { it.name.lowercase() }.toSet()

    override suspend fun knownHashes(onProgress: (Int, Int) -> Unit): MutableSet<String> {
        val known = HashSet<String>()
        val items = store.queryMedia()
        items.forEachIndexed { i, item ->
            currentCoroutineContext().ensureActive()
            onProgress(i, items.size)
            try {
                context.contentResolver.openInputStream(item.uri)?.use { known.add(sha256Of(it)) }
            } catch (e: Exception) {
            }
        }
        return known
    }

    override suspend fun resolveAlbum(name: String, merge: Boolean): Long {
        val list = albumList()
        val same = list.find { it.parentId == null && it.name.equals(name, ignoreCase = true) }
        if (same != null && merge) return same.id
        var finalName = name
        var n = 2
        while (list.any { it.parentId == null && it.name.equals(finalName, ignoreCase = true) }) finalName = "$name ${n++}"
        val id = dao.insertAlbum(AlbumEntity(name = finalName))
        list.add(AlbumEntity(id = id, name = finalName, createdAt = System.currentTimeMillis()))
        return id
    }

    override suspend fun resolveFolder(parentId: Long, name: String): Long {
        val list = albumList()
        val same = list.find { it.parentId == parentId && it.name.equals(name, ignoreCase = true) }
        if (same != null) return same.id
        val id = dao.insertAlbum(AlbumEntity(name = name, parentId = parentId))
        list.add(AlbumEntity(id = id, name = name, createdAt = System.currentTimeMillis(), parentId = parentId))
        return id
    }

    override suspend fun store(temp: File, entry: ImportEntry, date: Long): Any? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, entry.name)
            put(MediaStore.MediaColumns.MIME_TYPE, entry.mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, if (entry.isVideo) "Movies/" else "Pictures/")
            put(MediaStore.MediaColumns.DATE_TAKEN, if (entry.taken > 0L) entry.taken else date)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = if (entry.isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val newUri = resolver.insert(collection, values) ?: return null
        val copied = try {
            resolver.openOutputStream(newUri)?.use { out -> temp.inputStream().use { it.copyTo(out) } } != null
        } catch (e: Exception) {
            false
        }
        if (!copied) {
            resolver.delete(newUri, null, null)
            return null
        }
        resolver.update(
            newUri,
            ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
                put(MediaStore.MediaColumns.DATE_MODIFIED, date / 1000)
            },
            null,
            null
        )
        val id = ContentUris.parseId(newUri)
        // MediaStore stamps the new row with today, so the original date goes back as the sort date
        metadata.setSortDate(id, if (entry.added > 0L) entry.added else date / 1000)
        return id
    }

    override suspend fun addMember(albumId: Long, key: Any) {
        dao.insertMembership(org.nooklin.photos.data.local.AlbumMembershipEntity(albumId, key as Long))
    }
}

// Zip of the library. Flat: everything in Library/. Organized: Albums/Album/Folder/file, photos in no album in Library/.
suspend fun exportLibraryZip(
    context: Context,
    destination: Uri,
    organized: Boolean,
    onProgress: (done: Int, total: Int) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    try {
        val metadata = LocalMetadataStore(context)
        val recycled = metadata.recycleBinEntries.first().map { it.mediaId }.toSet()
        val items = MediaStoreRepository(context).queryMedia().filterNot { it.id in recycled }
        val dao = NooklinDatabase.getInstance(context).albumDao()
        val albums = dao.observeAlbums().first()
        val memberships = dao.observeMemberships().first().groupBy { it.albumId }
        val itemsById = items.associateBy { it.id }
        val inAnyAlbum = HashSet<Long>()

        // Entries per directory so two photos with the same name do not overwrite each other
        val usedNames = HashSet<String>()
        fun uniqueEntry(dir: String, item: MediaItem): String {
            val name = (item.displayName ?: "${item.id}.${fileExtensionFor(item.mimeType, item.isVideo)}")
                .replace(Regex("[/\\\\:*?\"<>|]"), "_")
            var candidate = "$dir/$name"
            var n = 2
            val base = name.substringBeforeLast('.', name)
            val ext = name.substringAfterLast('.', "")
            while (!usedNames.add(candidate)) {
                candidate = "$dir/$base ($n)" + if (ext.isNotEmpty()) ".$ext" else ""
                n++
            }
            return candidate
        }

        // Directory names of albums and folders, unique and safe in a zip
        val albumsById = albums.associateBy { it.id }
        fun cleanName(name: String) = name.replace(Regex("[/\\\\:*?\"<>|]"), "_").ifBlank { "Untitled" }
        val dirs = HashMap<Long, String>()
        val usedDirs = HashSet<String>()
        fun dirOf(album: AlbumEntity): String = dirs.getOrPut(album.id) {
            val parent = album.parentId?.let { albumsById[it] }
            val base = if (parent != null) dirOf(parent) + "/" + cleanName(album.name) else "Albums/" + cleanName(album.name)
            if (usedDirs.add(base)) base else "${base}_${album.id}".also { usedDirs.add(it) }
        }

        // The writes: entry name to item, decided first so progress has a total
        val writes = mutableListOf<Pair<String, MediaItem>>()
        if (organized) {
            albums.forEach { album ->
                memberships[album.id].orEmpty().forEach { m ->
                    val item = itemsById[m.mediaId] ?: return@forEach
                    inAnyAlbum.add(item.id)
                    writes.add(uniqueEntry(dirOf(album), item) to item)
                }
            }
            items.filter { it.id !in inAnyAlbum }.forEach { writes.add(uniqueEntry("Library", it) to it) }
        } else {
            items.forEach { writes.add(uniqueEntry("Library", it) to it) }
        }

        onProgress(0, writes.size)
        var done = 0
        context.contentResolver.openOutputStream(destination)?.use { out ->
            ZipOutputStream(out).use { zip ->
                // Photos and videos are compressed already
                zip.setLevel(Deflater.NO_COMPRESSION)
                // Entry names are made safe, the real name and dates travel in metadata.json
                val files = JSONObject()
                writes.forEach { (entryName, item) ->
                    files.put(entryName, JSONObject().apply {
                        item.displayName?.let { put("name", it) }
                        put("modified", item.dateModified * 1000)
                        item.dateTaken?.let { put("taken", it) }
                        put("added", item.dateAdded)
                    })
                }
                zip.putNextEntry(ZipEntry(METADATA_FILE))
                zip.write(JSONObject().put("version", 1).put("files", files).toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                writes.forEach { (entryName, item) ->
                    currentCoroutineContext().ensureActive()
                    val input = context.contentResolver.openInputStream(item.uri)
                    if (input != null) {
                        input.use {
                            val entry = ZipEntry(entryName)
                            entry.time = item.dateModified * 1000
                            zip.putNextEntry(entry)
                            it.copyTo(zip)
                            zip.closeEntry()
                        }
                    }
                    onProgress(++done, writes.size)
                }
            }
        } ?: return@withContext false
        true
    } catch (e: Exception) {
        // No partial zip left behind
        try { android.provider.DocumentsContract.deleteDocument(context.contentResolver, destination) } catch (_: Exception) { }
        false
    }
}
