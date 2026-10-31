package org.nooklin.photos.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.nooklin.photos.R
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.ui.common.GridThumbnail
import org.nooklin.photos.appString
import org.nooklin.photos.ui.theme.AppColors
import org.nooklin.photos.ui.theme.AppText
import org.nooklin.photos.ui.theme.Spacing

private val MenuRowHeight = 52.dp
private val MenuHeight = MenuRowHeight * 5 + Spacing.Sm * 2
private val CardMaxWidth = 320.dp

// Long press in the Library: the photo small on a dimmed background with a menu below it.
// A video shows its first frame. Tapping the card opens the viewer, tapping outside closes.
@Composable
fun PhotoPreviewMenu(
    item: MediaItem,
    shareEnabled: Boolean,
    hideEnabled: Boolean,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onDuplicate: () -> Unit,
    onHide: () -> Unit,
    onAddToAlbum: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null,
                    onClick = onDismiss
                )
                .padding(Spacing.Lg),
            contentAlignment = Alignment.Center
        ) {
            // Room left for the card once the menu has its height
            val availableHeight: Dp = (maxHeight - MenuHeight - Spacing.Lg).coerceAtLeast(120.dp)
            val availableWidth: Dp = minOf(maxWidth, CardMaxWidth)
            val ratio = if (item.width != null && item.height != null && item.width > 0 && item.height > 0) {
                item.width.toFloat() / item.height.toFloat()
            } else 1f
            var cardWidth = availableWidth
            var cardHeight = cardWidth / ratio
            if (cardHeight > availableHeight) {
                cardHeight = availableHeight
                cardWidth = cardHeight * ratio
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.Lg)
            ) {
                Box(
                    Modifier
                        .size(cardWidth, cardHeight)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(role = Role.Button, onClick = onOpen)
                ) {
                    GridThumbnail(
                        uri = item.uri,
                        dateModified = item.dateModified,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column(
                    Modifier
                        .widthIn(max = CardMaxWidth)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppColors.PopupDark)
                        // Taps on the menu background must not close the preview
                        .clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = {})
                        .padding(vertical = Spacing.Sm)
                ) {
                    Row(Modifier.fillMaxWidth().height(MenuRowHeight)) {
                        TopAction(appString(R.string.common_share), Icons.Filled.Share, shareEnabled, Color.White, onShare)
                        TopAction(
                            appString(R.string.common_favorite),
                            if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            true, Color.White, onFavorite
                        )
                        TopAction(appString(R.string.common_delete), Icons.Filled.Delete, true, AppColors.DestructiveText, onDelete)
                    }
                    if (shareEnabled) MenuRow(appString(R.string.photo_viewer_copy), Icons.Filled.ContentCopy, onCopy)
                    MenuRow(appString(R.string.photo_viewer_duplicate), Icons.Filled.ContentCopy, onDuplicate)
                    if (hideEnabled) MenuRow(appString(R.string.common_hide), Icons.Filled.VisibilityOff, onHide)
                    MenuRow(appString(R.string.common_add_to_album), Icons.Filled.PhotoAlbum, onAddToAlbum)
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TopAction(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val tint = if (enabled) color else color.copy(alpha = 0.4f)
    Column(
        Modifier
            .weight(1f)
            .height(MenuRowHeight)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, color = tint, fontSize = AppText.Caption)
    }
}

@Composable
private fun MenuRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(MenuRowHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.Lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White, fontSize = AppText.BodyLarge)
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}
