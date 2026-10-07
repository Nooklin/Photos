package org.nooklin.photos.ui.common

import android.content.Context
import coil3.request.ImageRequest

// Cache key includes the modified time: after an in-place edit the same Uri or path would serve the old image
fun freshImageRequest(context: Context, model: Any, freshnessKey: Any): ImageRequest {
    val key = "$model|$freshnessKey"
    return ImageRequest.Builder(context)
        .data(model)
        .memoryCacheKey(key)
        .diskCacheKey(key)
        .build()
}
