package org.nooklin.photos.ui.hidden

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Share
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
    onShare: (() -> Unit)? = null,
    extra: CardAction? = null
) {
    val context = LocalContext.current
    // every action closes the card, like the long press card in the Library
    fun closing(action: () -> Unit): () -> Unit = { action(); onDismiss() }
    CardMenu(
        ratio = item.aspectRatioOrUnknown(),
        onOpen = closing(onOpen),
        onDismiss = onDismiss,
        topActions = buildList {
            if (onShare != null) add(CardAction(appString(R.string.common_share), Icons.Filled.Share, onClick = closing(onShare)))
            add(
                CardAction(
                    appString(R.string.common_favorite),
                    if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    onClick = closing(onFavorite)
                )
            )
            add(CardAction(appString(R.string.common_unhide), Icons.Filled.Visibility, onClick = closing(onUnhide)))
            add(CardAction(appString(R.string.common_delete), Icons.Filled.Delete, destructive = true, onClick = closing(onDelete)))
        },
        rows = buildList {
            add(CardAction(appString(R.string.common_select), Icons.Filled.CheckCircle, onClick = closing(onSelect)))
            add(CardAction(appString(R.string.common_add_to_album), Icons.Filled.PhotoAlbum, onClick = closing(onAddToAlbum)))
            if (extra != null) add(CardAction(extra.label, extra.icon, extra.enabled, extra.destructive, extra.onDisabledClick, closing(extra.onClick)))
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
