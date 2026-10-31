package org.nooklin.photos.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.nooklin.photos.data.MediaItem

private val COLLAGE_GAP = 2.dp

// Collage of up to 4 cells with 2 dp between them and around them: 1 full, 2 side by side, 3 one large and two stacked, 4 a 2x2 grid.
// fillSlots (an album with 2 or more folders): always 4 cells, the ones without a photo are a plain area in the app's accent colour.
@Composable
fun CollageLayout(size: Dp, count: Int, fillSlots: Boolean, cell: @Composable (index: Int, modifier: Modifier) -> Unit) {
    val photos = count.coerceAtMost(4)
    val cells = if (fillSlots && photos >= 1) 4 else photos
    val placeholder = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    @Composable
    fun Slot(index: Int, modifier: Modifier) {
        if (index < photos) cell(index, modifier) else Box(modifier.background(placeholder))
    }
    Box(Modifier.size(size).clip(RoundedCornerShape(12.dp))) {
        when (cells) {
            0 -> {}
            1 -> cell(0, Modifier.fillMaxSize())
            2 -> Row(
                Modifier.fillMaxSize().padding(COLLAGE_GAP),
                horizontalArrangement = Arrangement.spacedBy(COLLAGE_GAP)
            ) {
                Slot(0, Modifier.weight(1f).fillMaxHeight())
                Slot(1, Modifier.weight(1f).fillMaxHeight())
            }
            3 -> Row(
                Modifier.fillMaxSize().padding(COLLAGE_GAP),
                horizontalArrangement = Arrangement.spacedBy(COLLAGE_GAP)
            ) {
                Slot(0, Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(COLLAGE_GAP)) {
                    Slot(1, Modifier.weight(1f).fillMaxWidth())
                    Slot(2, Modifier.weight(1f).fillMaxWidth())
                }
            }
            else -> Column(
                Modifier.fillMaxSize().padding(COLLAGE_GAP),
                verticalArrangement = Arrangement.spacedBy(COLLAGE_GAP)
            ) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(COLLAGE_GAP)) {
                    Slot(0, Modifier.weight(1f).fillMaxHeight())
                    Slot(1, Modifier.weight(1f).fillMaxHeight())
                }
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(COLLAGE_GAP)) {
                    Slot(2, Modifier.weight(1f).fillMaxHeight())
                    Slot(3, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
fun AlbumThumbnail(photos: List<MediaItem>, size: Dp, fillSlots: Boolean = false) {
    val context = LocalContext.current
    CollageLayout(size, photos.size, fillSlots) { index, modifier ->
        AsyncImage(
            model = freshImageRequest(context, photos[index].uri, photos[index].dateModified),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
