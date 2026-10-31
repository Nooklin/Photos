package org.nooklin.photos.ui.hidden

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import org.nooklin.photos.ui.common.CollageLayout
import org.nooklin.photos.ui.common.freshImageRequest
import java.io.File

// Same collage as ui.common.AlbumThumbnail, separate because that one takes MediaItems and hidden items are Files
@Composable
fun HiddenAlbumThumbnail(files: List<File>, size: Dp, fillSlots: Boolean = false) {
    val context = LocalContext.current
    CollageLayout(size, files.size, fillSlots) { index, modifier ->
        AsyncImage(
            model = freshImageRequest(context, files[index], files[index].lastModified()),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
