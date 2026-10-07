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
import coil3.size.Size

// Two layers: Android's thumbnail at once, the sharp decode fades in over it
@Composable
fun GridThumbnail(uri: Uri, dateModified: Any, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Sharp decode only after the cell stays 200ms, cells flying past are cancelled.
    // Cells that were sharp before show it at once.
    val sharpKey = "$uri|$dateModified"
    var showSharp by remember(sharpKey) { mutableStateOf(sharpKey in sharpShown) }
    if (!showSharp) {
        LaunchedEffect(sharpKey) {
            delay(SHARP_DELAY_MS)
            sharpShown.add(sharpKey)
            showSharp = true
        }
    }
    Box(modifier) {
        // A fixed small size always takes the fast Android thumbnail path, see MediaStoreThumbnailFetcher.
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .size(Size(QUICK_SIZE_PX, QUICK_SIZE_PX))
                .memoryCacheKey("$uri|$dateModified|quick")
                .diskCacheKey("$uri|$dateModified|quick")
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium,
            modifier = Modifier.fillMaxSize()
        )
        if (showSharp) {
            AsyncImage(
                model = freshImageRequest(context, uri, dateModified),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// How long a cell must stay on screen before its sharp decode starts.
private const val SHARP_DELAY_MS = 200L

// Photos that already had their sharp layer this session, so scrolling back does not wait again.
internal val sharpShown: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

// Below the large-cell threshold in MediaStoreThumbnailFetcher on any phone.
internal const val QUICK_SIZE_PX = 180
