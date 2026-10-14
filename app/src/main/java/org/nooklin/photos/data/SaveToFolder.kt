package org.nooklin.photos.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

data class SaveResult(val saved: Int, val skipped: Int, val failed: Int)

// Copies files into a folder the user picked (ACTION_OPEN_DOCUMENT_TREE). A null source means skipped.
fun saveToFolder(context: Context, treeUri: Uri, sources: List<Pair<MediaItem, Uri?>>): SaveResult {
    val resolver = context.contentResolver
    val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
    var saved = 0
    var skipped = 0
    var failed = 0
    for ((item, source) in sources) {
        if (source == null) {
            skipped++
            continue
        }
        val mime = resolver.getType(source) ?: item.mimeType ?: if (item.isVideo) "video/mp4" else "image/jpeg"
        // The original keeps its own name, a converted copy gets the new extension
        val originalName = item.displayName
        val name = if (source == item.uri && originalName != null) {
            originalName
        } else {
            val base = originalName?.substringBeforeLast('.') ?: "nooklin_${item.id}"
            "$base.${fileExtensionFor(mime, item.isVideo)}"
        }
        try {
            val target = DocumentsContract.createDocument(resolver, parent, mime, name)
            if (target == null) {
                failed++
                continue
            }
            val copied = resolver.openInputStream(source)?.use { input ->
                resolver.openOutputStream(target)?.use { output ->
                    input.copyTo(output)
                    true
                }
            } == true
            if (copied) {
                saved++
            } else {
                runCatching { DocumentsContract.deleteDocument(resolver, target) }
                failed++
            }
        } catch (e: Exception) {
            failed++
        }
    }
    return SaveResult(saved, skipped, failed)
}
