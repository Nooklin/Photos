package org.nooklin.photos.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.nooklin.photos.data.local.HiddenAlbumEntity
import org.nooklin.photos.data.local.HiddenAlbumMembershipEntity
import org.nooklin.photos.data.local.HiddenMediaEntity

// Import of a folder structure (or a zip of one) into the Hidden Section or into the library.
// First folder level = album, second level = folder in that album, deeper levels join the folder.

// Written first in every zip we export, keeps the original name and dates per entry
const val METADATA_FILE = "metadata.json"

// The place the files come from: a folder (tree Uri) or a zip file
class ImportSource(val uri: Uri, val isZip: Boolean)

// [ref] is the document Uri (folder) or the entry name (zip)
class ImportEntry(
    val album: String?,
    val folder: String?,
    val name: String,
    val modified: Long,
    val isVideo: Boolean,
    val mime: String,
    val ref: String,
    // From metadata.json in a zip of ours, 0 when unknown. Capture time in ms and added time in seconds.
    val taken: Long = 0L,
    val added: Long = 0L
)

class ImportResult(val imported: Int, val skipped: List<String>, val failed: Int, val notDeleted: Int)

// checking: comparing with what is already hidden, then importing
class ImportProgress(val checking: Boolean, val done: Int, val total: Int)

// "Hidden Images" holds loose photos in our own export, "Hidden Albums" is the album level of it
private fun mapPath(dirs: List<String>, isZip: Boolean): Pair<String?, String?> {
    var d = dirs
    if (d.firstOrNull() == "Hidden Images") d = emptyList()
    else if (d.firstOrNull() == "Hidden Albums") d = d.drop(1)
    // Our own library export, only trusted in a zip: a real folder named Albums stays an album
    else if (isZip && d.firstOrNull() == "Library") d = emptyList()
    else if (isZip && d.firstOrNull() == "Albums") d = d.drop(1)
    return d.getOrNull(0) to d.getOrNull(1)
}

internal fun mediaKind(name: String, providerMime: String?): String? {
    var mime = providerMime
    if (mime == null || mime == "application/octet-stream") {
        val ext = name.substringAfterLast('.', "").lowercase()
        mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }
    return mime?.takeIf { it.startsWith("image/") || it.startsWith("video/") }
}

private fun sha256(input: InputStream, copyTo: java.io.OutputStream? = null): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(64 * 1024)
    while (true) {
        val n = input.read(buffer)
        if (n < 0) break
        digest.update(buffer, 0, n)
        copyTo?.write(buffer, 0, n)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

suspend fun scanImportFolder(context: Context, tree: Uri): List<ImportEntry> =
    withContext(Dispatchers.IO) {
        val result = mutableListOf<ImportEntry>()
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        val stack = ArrayDeque<Pair<String, List<String>>>()
        stack.add(DocumentsContract.getTreeDocumentId(tree) to emptyList())
        while (stack.isNotEmpty()) {
            ensureActive()
            val (docId, dirs) = stack.removeLast()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
            context.contentResolver.query(children, columns, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val name = cursor.getString(1) ?: continue
                    val mime = cursor.getString(2)
                    val modified = if (cursor.isNull(3)) 0L else cursor.getLong(3)
                    if (name.startsWith(".")) continue
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        stack.add(id to (dirs + name))
                    } else {
                        val kind = mediaKind(name, mime) ?: continue
                        val (album, folder) = mapPath(dirs, false)
                        result.add(
                            ImportEntry(
                                album, folder, name, modified, kind.startsWith("video/"), kind,
                                DocumentsContract.buildDocumentUriUsingTree(tree, id).toString()
                            )
                        )
                    }
                }
            }
        }
        result
    }

suspend fun scanImportZip(context: Context, zip: Uri): List<ImportEntry> =
    withContext(Dispatchers.IO) {
        val result = mutableListOf<ImportEntry>()
        var meta: JSONObject? = null
        context.contentResolver.openInputStream(zip)?.use { raw ->
            ZipInputStream(raw).use { zis ->
                while (true) {
                    ensureActive()
                    val entry = zis.nextEntry ?: break
                    if (entry.isDirectory) continue
                    if (entry.name == METADATA_FILE) {
                        try { meta = JSONObject(String(zis.readBytes(), Charsets.UTF_8)).optJSONObject("files") } catch (e: Exception) { }
                        continue
                    }
                    val parts = entry.name.split('/').filter { it.isNotEmpty() }
                    val name = parts.lastOrNull() ?: continue
                    if (name.startsWith(".") || parts.firstOrNull() == "__MACOSX") continue
                    val kind = mediaKind(name, null) ?: continue
                    val (album, folder) = mapPath(parts.dropLast(1), true)
                    result.add(
                        ImportEntry(album, folder, name, entry.time.coerceAtLeast(0L), kind.startsWith("video/"), kind, entry.name)
                    )
                }
            }
        }
        // The original name and dates from metadata.json win over the zip entry, which keeps one date and a safe name
        val files = meta
        if (files == null) result else result.map { e ->
            val m = files.optJSONObject(e.ref) ?: return@map e
            ImportEntry(
                e.album, e.folder, m.optString("name").ifBlank { e.name },
                m.optLong("modified", 0L).takeIf { it > 0L } ?: e.modified,
                e.isVideo, e.mime, e.ref,
                m.optLong("taken", 0L), m.optLong("added", 0L)
            )
        }
    }

// Where the imported files go: the Hidden Section or the library. The same import runs for both.
interface ImportSink {
    // Names (lower case) of the albums that exist now, not folders
    suspend fun topLevelAlbumNames(): Set<String>

    // SHA-256 of everything that is there already
    suspend fun knownHashes(onProgress: (done: Int, total: Int) -> Unit): MutableSet<String>

    suspend fun resolveAlbum(name: String, merge: Boolean): Long
    suspend fun resolveFolder(parentId: Long, name: String): Long

    // Takes the copy in [temp] (the caller deletes it). Null when it failed, else the key of the new item.
    suspend fun store(temp: File, entry: ImportEntry, date: Long): Any?
    suspend fun addMember(albumId: Long, key: Any)
}

// A file that is already there (same content) is skipped and listed. [merge]: an album with the same name gets the
// photos, otherwise a new album "Name 2" is made. [deleteAfter]: imported files of a folder are removed from it.
suspend fun runImport(
    context: Context,
    sink: ImportSink,
    source: ImportSource,
    entries: List<ImportEntry>,
    merge: Boolean,
    deleteAfter: Boolean,
    onProgress: (ImportProgress) -> Unit
): ImportResult = withContext(Dispatchers.IO) {
    val known = sink.knownHashes { done, total -> onProgress(ImportProgress(true, done, total)) }

    val albumIds = HashMap<String, Long>()
    suspend fun albumId(name: String): Long = albumIds.getOrPut(name) { sink.resolveAlbum(name, merge) }
    val folderIds = HashMap<Pair<Long, String>, Long>()
    suspend fun folderId(parent: Long, name: String): Long =
        folderIds.getOrPut(parent to name) { sink.resolveFolder(parent, name) }

    var imported = 0
    var failed = 0
    var notDeleted = 0
    val skipped = mutableListOf<String>()
    var done = 0

    // One file: copy while hashing, skip when known, else store it and put it in its album or folder
    suspend fun importOne(entry: ImportEntry, input: InputStream): Boolean {
        val temp = File(context.cacheDir, "import_${System.nanoTime()}")
        try {
            val hash = try {
                temp.outputStream().use { out -> sha256(input, out) }
            } catch (e: Exception) {
                failed++
                return false
            }
            if (temp.length() <= 0L) { failed++; return false }
            if (!known.add(hash)) {
                skipped.add(listOfNotNull(entry.album, entry.folder, entry.name).joinToString(" / "))
                return false
            }
            // The original date stays
            val date = if (entry.modified > 0L) entry.modified else System.currentTimeMillis()
            val key = sink.store(temp, entry, date)
            if (key == null) { failed++; return false }
            val target = when {
                entry.album == null -> null
                entry.folder == null -> albumId(entry.album)
                else -> folderId(albumId(entry.album), entry.folder)
            }
            if (target != null) sink.addMember(target, key)
            imported++
            return true
        } finally {
            temp.delete()
        }
    }

    onProgress(ImportProgress(false, 0, entries.size))
    if (source.isZip) {
        val wanted = entries.associateBy { it.ref }
        var handled = 0
        context.contentResolver.openInputStream(source.uri)?.use { raw ->
            ZipInputStream(raw).use { zis ->
                while (true) {
                    ensureActive()
                    val zipEntry = zis.nextEntry ?: break
                    val entry = wanted[zipEntry.name] ?: continue
                    handled++
                    // The stream must stay open for the next entry, so it is not closed here
                    importOne(entry, object : java.io.FilterInputStream(zis) { override fun close() {} })
                    onProgress(ImportProgress(false, ++done, entries.size))
                }
            }
        }
        // Chosen files that were not found in the zip count as failed, not as nothing
        failed += (entries.size - handled).coerceAtLeast(0)
    } else {
        entries.forEach { entry ->
            ensureActive()
            val uri = Uri.parse(entry.ref)
            val input = try { context.contentResolver.openInputStream(uri) } catch (e: Exception) { null }
            if (input == null) {
                failed++
            } else {
                val ok = input.use { importOne(entry, it) }
                if (ok && deleteAfter) {
                    val deleted = try { DocumentsContract.deleteDocument(context.contentResolver, uri) } catch (e: Exception) { false }
                    if (!deleted) notDeleted++
                }
            }
            onProgress(ImportProgress(false, ++done, entries.size))
        }
    }
    ImportResult(imported, skipped, failed, notDeleted)
}

// Into the Hidden Section
class HiddenImportSink(private val context: Context, private val repository: HiddenMediaRepository) : ImportSink {
    private val dao = repository.hiddenDao
    private var albums: MutableList<HiddenAlbumEntity>? = null

    private suspend fun albumList(): MutableList<HiddenAlbumEntity> =
        albums ?: dao.allAlbums().toMutableList().also { albums = it }

    override suspend fun topLevelAlbumNames(): Set<String> =
        dao.allAlbums().filter { it.parentId == null }.map { it.name.lowercase() }.toSet()

    override suspend fun knownHashes(onProgress: (Int, Int) -> Unit): MutableSet<String> {
        val known = HashSet<String>()
        // Only files that really are in the Hidden library (also the recycle bin). Leftovers without a database row are removed.
        val rows = dao.allMedia().map { it.fileName }.toSet()
        val recent = System.currentTimeMillis() - 60_000L
        val existing = repository.hiddenDir.listFiles().orEmpty().filter { it.isFile }
        existing.forEachIndexed { i, file ->
            currentCoroutineContext().ensureActive()
            onProgress(i, existing.size)
            if (file.name !in rows) {
                if (file.lastModified() < recent) {
                    file.delete()
                    File(repository.thumbsDir, file.name).delete()
                }
                return@forEachIndexed
            }
            try { file.inputStream().use { known.add(sha256(it)) } } catch (e: Exception) { }
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
        val id = dao.insertAlbum(HiddenAlbumEntity(name = finalName))
        list.add(HiddenAlbumEntity(id = id, name = finalName))
        return id
    }

    override suspend fun resolveFolder(parentId: Long, name: String): Long {
        val list = albumList()
        val same = list.find { it.parentId == parentId && it.name.equals(name, ignoreCase = true) }
        if (same != null) return same.id
        val id = dao.insertAlbum(HiddenAlbumEntity(name = name, parentId = parentId))
        list.add(HiddenAlbumEntity(id = id, name = name, parentId = parentId))
        return id
    }

    override suspend fun store(temp: File, entry: ImportEntry, date: Long): Any? {
        val extension = fileExtensionFor(entry.mime, entry.isVideo)
        val destination = File(repository.hiddenDir, "imp_${System.nanoTime()}.$extension")
        try {
            temp.copyTo(destination, overwrite = true)
        } catch (e: Exception) {
            destination.delete()
            return null
        }
        destination.setLastModified(date)
        if (entry.isVideo) {
            repository.generateVideoThumbnail(destination, File(repository.thumbsDir, destination.name))
        } else {
            repository.generatePhotoThumbnail(Uri.fromFile(destination), File(repository.thumbsDir, destination.name))
        }
        dao.insertMedia(
            HiddenMediaEntity(
                fileName = destination.name,
                isVideo = entry.isVideo,
                hiddenAt = date,
                mimeType = entry.mime,
                dateAdded = if (entry.added > 0L) entry.added else date / 1000,
                originalName = entry.name,
                originalModified = entry.modified.takeIf { it > 0L },
                originalTaken = entry.taken.takeIf { it > 0L }
            )
        )
        return destination.name
    }

    override suspend fun addMember(albumId: Long, key: Any) {
        dao.addMembership(HiddenAlbumMembershipEntity(albumId, key as String))
    }
}
