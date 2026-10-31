package org.nooklin.photos

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore

// What another app asked this app to do: show one file, or let the user choose files to hand back
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

// Result for a Pick: the chosen files with read access for the caller
fun List<Uri>.toPickResult(): Intent {
    val clip = ClipData.newRawUri(null, first())
    drop(1).forEach { clip.addItem(ClipData.Item(it)) }
    return Intent().apply {
        data = first()
        clipData = clip
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

// The library id of a content://media/external/images|video/media/<id> Uri, null for any other Uri
// (a file provider Uri of another app, or a photo picker Uri whose id means something else)
fun Uri.toMediaStoreId(): Long? {
    if (scheme != "content" || authority != MediaStore.AUTHORITY) return null
    val segments = pathSegments
    if (segments.size != 4) return null
    if (!segments[0].startsWith("external")) return null
    if (segments[1] != "images" && segments[1] != "video") return null
    if (segments[2] != "media") return null
    return segments[3].toLongOrNull()
}
