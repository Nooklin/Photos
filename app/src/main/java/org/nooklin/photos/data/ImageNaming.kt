package org.nooklin.photos.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore

object ImageNaming {
    private val lastIssued = HashMap<String, Int>()

    @Synchronized
    private fun nextNumber(context: Context, collection: Uri, prefix: String): Int {
        val pattern = Regex("${prefix}_(\\d+)\\.[A-Za-z0-9]+")
        var highest = 0
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
            arrayOf("${prefix}_%"),
            null
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn) ?: continue
                val number = pattern.matchEntire(name)?.groupValues?.get(1)?.toIntOrNull() ?: continue
                if (number > highest) highest = number
            }
        }
        val next = maxOf(highest, lastIssued[prefix] ?: 0) + 1
        lastIssued[prefix] = next
        return next
    }

    fun fileName(context: Context, extension: String): String =
        "IMG_%04d.%s".format(nextNumber(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "IMG"), extension)

    fun videoFileName(context: Context, extension: String): String =
        "VID_%04d.%s".format(nextNumber(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "VID"), extension)
}
