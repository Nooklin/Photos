package org.nooklin.photos.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.io.File
import java.io.InputStream
import java.io.OutputStream

// Untouched original, kept aside on the first edit for Revert
class OriginalsStore(private val context: Context) {
    private val dir = File(context.filesDir, "originals")

    companion object {
        fun mediaKey(mediaId: Long) = "media_$mediaId"
        fun hiddenKey(fileName: String) = "hidden_$fileName"
    }

    fun has(key: String): Boolean = File(dir, key).let { it.isFile && it.length() > 0L }

    // Temp file then rename, so an interrupted copy is never taken for a valid original
    fun stashIfAbsent(key: String, source: () -> InputStream?): Boolean {
        if (has(key)) return true
        dir.mkdirs()
        val tmp = File(dir, "$key.tmp")
        return try {
            val copied = source()?.use { input -> tmp.outputStream().use { out -> input.copyTo(out) } } ?: -1L
            if (copied > 0L && tmp.length() == copied && tmp.renameTo(File(dir, key))) {
                true
            } else {
                tmp.delete()
                false
            }
        } catch (e: Exception) {
            tmp.delete()
            false
        }
    }

    fun copyTo(key: String, output: OutputStream): Boolean = try {
        val f = File(dir, key)
        f.isFile && f.inputStream().use { it.copyTo(output) } == f.length()
    } catch (e: Exception) {
        false
    }

    fun copy(fromKey: String, toKey: String) {
        if (!has(fromKey) || has(toKey)) return
        try {
            File(dir, fromKey).copyTo(File(dir, toKey), overwrite = false)
        } catch (e: Exception) {
            // Best effort, a lost Revert should not fail hide or unhide
        }
    }

    fun delete(key: String) {
        File(dir, key).delete()
    }

    // Hidden originals are removed with the hidden file, not here
    fun pruneMedia(liveIds: Set<Long>) {
        // Partial media access gives an incomplete query, originals are not recoverable
        val fullAccess = listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            .all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (!fullAccess) return
        dir.listFiles()?.forEach { f ->
            if (!f.name.startsWith("media_")) return@forEach
            val id = f.name.removePrefix("media_").removeSuffix(".tmp").toLongOrNull()
            if (f.name.endsWith(".tmp") || id == null || id !in liveIds) f.delete()
        }
    }
}
