package org.nooklin.photos.ui.hidden

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.HiddenItem
import org.nooklin.photos.ui.common.aspectRatioOrUnknown
import org.nooklin.photos.ui.common.freshImageRequest
import org.nooklin.photos.ui.gallery.CardAction
import org.nooklin.photos.ui.gallery.CardMenu
import androidx.compose.ui.platform.LocalContext

@Composable
fun HiddenItemMenu(
    item: HiddenItem,
    onOpen: () -> Unit,
    onSelect: () -> Unit,
    onFavorite: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit,
    onAddToAlbum: () -> Unit,
    onDismiss: () -> Unit,
    extra: CardAction? = null
) {
    val context = LocalContext.current
    CardMenu(
        ratio = item.aspectRatioOrUnknown(),
        onOpen = onOpen,
        onDismiss = onDismiss,
        topActions = listOf(
            CardAction(
                appString(R.string.common_favorite),
                if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                onClick = onFavorite
            ),
            CardAction(appString(R.string.common_unhide), Icons.Filled.Visibility, onClick = onUnhide),
            CardAction(appString(R.string.common_delete), Icons.Filled.Delete, destructive = true, onClick = onDelete)
        ),
        rows = buildList {
            add(CardAction(appString(R.string.common_select), Icons.Filled.CheckCircle, onClick = onSelect))
            add(CardAction(appString(R.string.common_add_to_album), Icons.Filled.PhotoAlbum, onClick = onAddToAlbum))
            if (extra != null) add(extra)
        }
    ) {
        AsyncImage(
            model = freshImageRequest(context, item.thumbnailFile, item.thumbnailFile.lastModified()),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
