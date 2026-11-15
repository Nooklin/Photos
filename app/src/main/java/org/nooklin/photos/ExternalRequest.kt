package org.nooklin.photos

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore

sealed interface ExternalRequest {
    data class View(val uri: Uri, val mimeType: String?) : ExternalRequest

    data class Pick(val allowMultiple: Boolean, val images: Boolean, val videos: Boolean) : ExternalRequest
}

fun Intent.toExternalRequest(context: Context): ExternalRequest? = when (action) {
    Intent.ACTION_VIEW -> data?.let { uri ->
        ExternalRequest.View(uri, type ?: context.contentResolver.getType(uri))
    }
    Intent.ACTION_PICK, Intent.ACTION_GET_CONTENT -> {
        val requested = type.orEmpty()
        ExternalRequest.Pick(
            allowMultiple = getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false),
            images = !requested.startsWith("video/"),
            videos = !requested.startsWith("image/")
        )
    }
    else -> null
}

fun List<Uri>.toPickResult(): Intent {
    val clip = ClipData.newRawUri(null, first())
    drop(1).forEach { clip.addItem(ClipData.Item(it)) }
    return Intent().apply {
        data = first()
        clipData = clip
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

fun Uri.toMediaStoreId(): Long? {
    if (scheme != "content" || authority != MediaStore.AUTHORITY) return null
    val segments = pathSegments
    if (segments.size != 4) return null
    if (!segments[0].startsWith("external")) return null
    if (segments[1] != "images" && segments[1] != "video") return null
    if (segments[2] != "media") return null
    return segments[3].toLongOrNull()
}
