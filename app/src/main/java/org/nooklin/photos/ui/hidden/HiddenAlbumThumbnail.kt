package org.nooklin.photos.ui.hidden

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.nooklin.photos.ui.common.freshImageRequest
import java.io.File

// Same collage as ui.common.AlbumThumbnail, separate because that one takes MediaItems and hidden items are Files
@Composable
fun HiddenAlbumThumbnail(files: List<File>, size: Dp) {
    val context = LocalContext.current
    Box(Modifier.size(size).clip(RoundedCornerShape(12.dp))) {
        when (files.size) {
            0 -> {}
            1 -> {
                AsyncImage(
                    model = freshImageRequest(context, files[0], files[0].lastModified()),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            2 -> {
                Row(Modifier.fillMaxSize()) {
                    files.forEach { file ->
                        AsyncImage(
                            model = freshImageRequest(context, file, file.lastModified()),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
            3 -> {
                Row(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = freshImageRequest(context, files[0], files[0].lastModified()),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        AsyncImage(
                            model = freshImageRequest(context, files[1], files[1].lastModified()),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                        AsyncImage(
                            model = freshImageRequest(context, files[2], files[2].lastModified()),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                }
            }
            else -> {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = freshImageRequest(context, files[0], files[0].lastModified()),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        AsyncImage(
                            model = freshImageRequest(context, files[1], files[1].lastModified()),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = freshImageRequest(context, files[2], files[2].lastModified()),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        AsyncImage(
                            model = freshImageRequest(context, files[3], files[3].lastModified()),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}
