package org.nooklin.photos.ui.common

import android.content.Context
import coil3.request.ImageRequest

// Big picture at the top of an album screen. Own cache key and screen size: with the key of the grid it
// showed the small grid thumbnail, blurry when stretched over the screen width.
fun heroImageRequest(context: Context, model: Any, freshnessKey: Any): ImageRequest {
    val key = "hero|$model|$freshnessKey"
    return ImageRequest.Builder(context)
        .data(model)
        .size(screenTierSize(context))
        .memoryCacheKey(key)
        .diskCacheKey(key)
        .build()
}

// Cache key includes the modified time: after an in-place edit the same Uri or path would serve the old image
fun freshImageRequest(context: Context, model: Any, freshnessKey: Any): ImageRequest {
    val key = "$model|$freshnessKey"
    return ImageRequest.Builder(context)
        .data(model)
        .memoryCacheKey(key)
        .diskCacheKey(key)
        .build()
}
