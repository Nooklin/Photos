package org.nooklin.photos.ui.common

import android.content.Context
import coil3.request.ImageRequest

// Big picture at the top of an album screen
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
        // files come from the private Hidden folders: no copy of them in the cache directory
        .apply { if (model is java.io.File) diskCachePolicy(coil3.request.CachePolicy.DISABLED) }
        .build()
}
