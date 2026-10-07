package org.nooklin.photos.data

import android.webkit.MimeTypeMap

fun fileExtensionFor(mimeType: String?, isVideo: Boolean): String {
    when (mimeType?.lowercase()) {
        "image/jpeg", "image/jpg" -> return "jpg"
        "image/png" -> return "png"
        "image/webp" -> return "webp"
        "image/heic" -> return "heic"
        "image/heif" -> return "heif"
        "image/gif" -> return "gif"
    }
    val fromMime = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
    return fromMime ?: if (isVideo) "mp4" else "jpg"
}
