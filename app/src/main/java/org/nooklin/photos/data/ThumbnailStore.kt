package org.nooklin.photos.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.security.MessageDigest

// Small thumbnails of the library kept in the app's private storage, so a cold start or a new import does not decode again
object ThumbnailStore {
    const val STORE_PX = 360
    private const val MAX_BYTES = 1_000_000_000L

    fun keyOf(item: MediaItem): String = "${item.uri}|${item.dateModified}"

    private fun dir(context: Context) = File(context.filesDir, "thumbs").apply { mkdirs() }

    private fun fileFor(context: Context, key: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(dir(context), "$hash.webp")
    }

    fun has(context: Context, key: String): Boolean = fileFor(context, key).exists()

    fun read(context: Context, key: String): Bitmap? {
        val file = fileFor(context, key)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath) ?: run { file.delete(); null }
    }

    fun write(context: Context, key: String, bitmap: Bitmap) {
        val file = fileFor(context, key)
        val tmp = File(file.parentFile, "${file.name}.${Thread.currentThread().id}.tmp")
        val short = minOf(bitmap.width, bitmap.height)
        val scaled = if (short > STORE_PX * 1.2f) {
            val scale = STORE_PX.toFloat() / short
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
        } else {
            bitmap
        }
        try {
            val ok = tmp.outputStream().use { scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, it) }
            if (ok && tmp.renameTo(file)) return
            tmp.delete()
        } catch (e: Exception) {
            tmp.delete()
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }

    // Thumbnails of items that are gone or changed, then the oldest ones when the folder is over its limit
    fun cleanUp(context: Context, items: List<MediaItem>) {
        if (items.isEmpty()) return
        val valid = HashSet<String>(items.size * 2)
        items.forEach { valid.add(fileFor(context, keyOf(it)).name) }
        val files = dir(context).listFiles() ?: return
        files.forEach { file ->
            if (file.name.endsWith(".tmp") || file.name !in valid) file.delete()
        }
        val left = dir(context).listFiles()?.sortedBy { it.lastModified() }.orEmpty()
        var total = left.sumOf { it.length() }
        for (file in left) {
            if (total <= MAX_BYTES) break
            total -= file.length()
            file.delete()
        }
    }
}
