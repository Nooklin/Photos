package org.nooklin.photos.ui.common

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
import org.nooklin.photos.data.MediaItem

// Collage: 1 full, 2 side by side, 3 one large and two stacked, 4+ a 2x2 grid
@Composable
fun AlbumThumbnail(photos: List<MediaItem>, size: Dp) {
    val context = LocalContext.current
    Box(Modifier.size(size).clip(RoundedCornerShape(12.dp))) {
        when (photos.size) {
            0 -> {}
            1 -> {
                AsyncImage(
                    model = freshImageRequest(context, photos[0].uri, photos[0].dateModified),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            2 -> {
                Row(Modifier.fillMaxSize()) {
                    photos.forEach { photo ->
                        AsyncImage(
                            model = freshImageRequest(context, photo.uri, photo.dateModified),
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
                        model = freshImageRequest(context, photos[0].uri, photos[0].dateModified),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        AsyncImage(
                            model = freshImageRequest(context, photos[1].uri, photos[1].dateModified),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                        AsyncImage(
                            model = freshImageRequest(context, photos[2].uri, photos[2].dateModified),
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
                            model = freshImageRequest(context, photos[0].uri, photos[0].dateModified),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        AsyncImage(
                            model = freshImageRequest(context, photos[1].uri, photos[1].dateModified),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(
                            model = freshImageRequest(context, photos[2].uri, photos[2].dateModified),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        AsyncImage(
                            model = freshImageRequest(context, photos[3].uri, photos[3].dateModified),
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
