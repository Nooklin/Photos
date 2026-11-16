package org.nooklin.photos.ui.hidden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import org.nooklin.photos.ui.common.CollageLayout
import org.nooklin.photos.ui.common.freshImageRequest
import java.io.File

@Composable
fun HiddenAlbumThumbnail(
    files: List<File>,
    size: Dp,
    fillSlots: Boolean = false,
    title: String? = null,
    subtitle: String? = null,
    corner: Dp = 12.dp,
    previewOff: Boolean = false
) {
    if (previewOff) {
        Box(
            Modifier.size(size).clip(RoundedCornerShape(corner)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.PhotoAlbum, contentDescription = null, modifier = Modifier.size(size / 3))
        }
        return
    }
    val context = LocalContext.current
    CollageLayout(size, files.size, fillSlots, title, subtitle, corner) { index, modifier ->
        AsyncImage(
            model = freshImageRequest(context, files[index], files[index].lastModified()),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
