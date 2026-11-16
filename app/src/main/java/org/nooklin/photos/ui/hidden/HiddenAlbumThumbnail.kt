package org.nooklin.photos.ui.hidden

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
    corner: Dp = 12.dp
) {
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
