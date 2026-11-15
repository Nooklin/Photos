package org.nooklin.photos.ui.common

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size

@Composable
fun GridThumbnail(uri: Uri, dateModified: Any, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Sharp decode only after the cell stays 100ms, cells flying past are cancelled
    val sharpKey = "$uri|$dateModified"
    var showSharp by remember(sharpKey) { mutableStateOf(sharpKey in sharpShownKeys) }
    if (!showSharp) {
        LaunchedEffect(sharpKey) {
            delay(100L)
            showSharp = true
        }
    }
    val quickRequest = remember(uri, dateModified) {
        ImageRequest.Builder(context)
            .data(uri)
            .size(Size(QUICK_SIZE_PX, QUICK_SIZE_PX))
            .memoryCacheKey("$uri|$dateModified|quick")
            .diskCacheKey("$uri|$dateModified|quick")
            .build()
    }
    val sharpRequest = remember(uri, dateModified) {
        freshImageRequest(context, uri, dateModified).newBuilder()
            // only now the sharp layer really exists, so the prefetch must not skip it before
            .listener(object : ImageRequest.Listener {
                override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                    markSharpShown(sharpKey)
                }
            })
            .build()
    }
    Box(modifier) {
        AsyncImage(
            model = quickRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium,
            modifier = Modifier.fillMaxSize()
        )
        if (showSharp) {
            AsyncImage(
                model = sharpRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// capped, oldest keys go first; a key only says "decoded recently", Coil may have evicted it since
private val sharpShownKeys: MutableSet<String> = java.util.Collections.synchronizedSet(
    java.util.Collections.newSetFromMap(object : LinkedHashMap<String, Boolean>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?) = size > 2000
    })
)

internal val sharpShown: Set<String> get() = sharpShownKeys

internal fun markSharpShown(key: String) {
    sharpShownKeys.add(key)
}

internal const val QUICK_SIZE_PX = 180
