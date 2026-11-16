package org.nooklin.photos.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.nooklin.photos.data.MediaItem

private val COLLAGE_GAP = 2.dp

@Composable
fun CollageLayout(
    size: Dp,
    count: Int,
    fillSlots: Boolean,
    title: String? = null,
    subtitle: String? = null,
    corner: Dp = 12.dp,
    cell: @Composable (index: Int, modifier: Modifier) -> Unit
) {
    val photos = count.coerceAtMost(4)
    val cells = if (fillSlots && photos >= 1) 4 else photos
    val placeholder = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    @Composable
    fun Slot(index: Int, modifier: Modifier) {
        if (index < photos) cell(index, modifier) else Box(modifier.background(placeholder))
    }
    Box(Modifier.size(size).clip(RoundedCornerShape(corner)).then(if (title != null && photos == 0) Modifier.background(placeholder) else Modifier)) {
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
        if (title != null) TitleOverlay(title, subtitle)
    }
}

@Composable
private fun BoxScope.TitleOverlay(title: String, subtitle: String?) {
    Column(
        Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
            .padding(start = 10.dp, end = 10.dp, top = 18.dp, bottom = 8.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) Text(subtitle, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
fun AlbumThumbnail(
    photos: List<MediaItem>,
    size: Dp,
    fillSlots: Boolean = false,
    title: String? = null,
    subtitle: String? = null,
    corner: Dp = 12.dp
) {
    val context = LocalContext.current
    CollageLayout(size, photos.size, fillSlots, title, subtitle, corner) { index, modifier ->
        AsyncImage(
            model = freshImageRequest(context, photos[index].uri, photos[index].dateModified),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
