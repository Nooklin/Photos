package org.nooklin.photos.ui.gallery

import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.data.local.AppPreferences
import kotlinx.coroutines.launch
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import org.nooklin.photos.ui.common.viewerCacheKey
import org.nooklin.photos.ui.common.screenTierSize
import coil3.request.maxBitmapSize
import coil3.request.crossfade
import coil3.request.ImageRequest
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
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
import androidx.compose.material.icons.filled.CheckCircle
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
import org.nooklin.photos.appString
import org.nooklin.photos.ui.theme.AppColors
import org.nooklin.photos.ui.theme.AppText
import org.nooklin.photos.ui.theme.Spacing

private val MenuRowHeight = 52.dp
private val CardMaxWidth = 320.dp

class CardAction(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
    val onDisabledClick: (() -> Unit)? = null,
    val onClick: () -> Unit
)

// Long-press card used on every grid: a preview on top, an icon row and text rows below
@Composable
fun CardMenu(
    ratio: Float,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    topActions: List<CardAction> = emptyList(),
    rows: List<CardAction>,
    preview: @Composable () -> Unit
) {
    val menuHeight = MenuRowHeight * (rows.size + if (topActions.isEmpty()) 0 else 1) + Spacing.Sm * 2
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
            val availableHeight: Dp = (maxHeight - menuHeight - Spacing.Lg).coerceAtLeast(120.dp)
            val availableWidth: Dp = minOf(maxWidth, CardMaxWidth)
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
                    preview()
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
                    if (topActions.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().height(MenuRowHeight)) {
                            topActions.forEach { action ->
                                TopAction(
                                    action.label, action.icon, action.enabled,
                                    if (action.destructive) AppColors.DestructiveText else Color.White,
                                    if (action.enabled) action.onClick else action.onDisabledClick
                                )
                            }
                        }
                    }
                    rows.forEach { row ->
                        MenuRow(row.label, row.icon, row.destructive, row.enabled, if (row.enabled) row.onClick else row.onDisabledClick)
                    }
                }
            }
        }
    }
}

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
    onSelect: () -> Unit,
    onDismiss: () -> Unit,
    copyEnabled: Boolean = shareEnabled,
    duplicateEnabled: Boolean = true,
    extra: CardAction? = null
) {
    val ratio = if (item.width != null && item.height != null && item.width > 0 && item.height > 0) {
        item.width.toFloat() / item.height.toFloat()
    } else 1f
    val context = LocalContext.current
    val noticeHidden by remember { AppPreferences(context).sharingNoticeHidden }.collectAsState(initial = true)
    var showNotice by remember { mutableStateOf(false) }
    val onBlocked: (() -> Unit)? = if (noticeHidden) null else ({ showNotice = true })
    val rows = buildList {
        add(CardAction(appString(R.string.common_select), Icons.Filled.CheckCircle, onClick = onSelect))
        add(CardAction(appString(R.string.common_add_to_album), Icons.Filled.PhotoAlbum, onClick = onAddToAlbum))
        if (copyEnabled) add(CardAction(appString(R.string.photo_viewer_copy), Icons.Filled.ContentCopy, onClick = onCopy))
        if (duplicateEnabled) add(CardAction(appString(R.string.photo_viewer_duplicate), Icons.Filled.ContentCopy, onClick = onDuplicate))
        if (hideEnabled) add(CardAction(appString(R.string.common_hide), Icons.Filled.VisibilityOff, onClick = onHide))
        if (extra != null) add(extra)
    }
    if (showNotice) SharingDisabledDialog(onDismiss = { showNotice = false })
    CardMenu(
        ratio = ratio,
        onOpen = onOpen,
        onDismiss = onDismiss,
        topActions = listOf(
            CardAction(appString(R.string.common_share), Icons.Filled.Share, shareEnabled, onDisabledClick = onBlocked, onClick = onShare),
            CardAction(
                appString(R.string.common_favorite),
                if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                onClick = onFavorite
            ),
            CardAction(appString(R.string.common_delete), Icons.Filled.Delete, destructive = true, onClick = onDelete)
        ),
        rows = rows
    ) {
        val size = remember { screenTierSize(context) }
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .size(size)
                .maxBitmapSize(size)
                .memoryCacheKey(viewerCacheKey(item.uri, item.dateModified, "screen"))
                .diskCacheKey(viewerCacheKey(item.uri, item.dateModified, "screen"))
                .placeholderMemoryCacheKey("${item.uri}|${item.dateModified}|quick")
                .crossfade(false)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TopAction(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    color: Color,
    onClick: (() -> Unit)?
) {
    val tint = if (enabled) color else color.copy(alpha = 0.4f)
    Column(
        Modifier
            .weight(1f)
            .height(MenuRowHeight)
            .clickable(enabled = onClick != null, role = Role.Button, onClick = { onClick?.invoke() }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, color = tint, fontSize = AppText.Caption)
    }
}

@Composable
private fun MenuRow(label: String, icon: ImageVector, destructive: Boolean, enabled: Boolean, onClick: (() -> Unit)?) {
    val base = if (destructive) AppColors.DestructiveText else Color.White
    val tint = if (enabled) base else base.copy(alpha = 0.4f)
    Row(
        Modifier
            .fillMaxWidth()
            .height(MenuRowHeight)
            .clickable(enabled = onClick != null, role = Role.Button, onClick = { onClick?.invoke() })
            .padding(horizontal = Spacing.Lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = tint, fontSize = AppText.BodyLarge)
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SharingDisabledDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dontShow by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = GlassPill.shape,
        containerColor = GlassPill.adaptivePopupBackground,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(appString(R.string.sharing_off_title)) },
        text = {
            Column {
                Text(appString(R.string.sharing_off_text))
                Row(
                    Modifier
                        .padding(top = Spacing.Md)
                        .clickable(role = Role.Checkbox) { dontShow = !dontShow },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = dontShow, onCheckedChange = null)
                    Text(appString(R.string.sharing_off_dont_show), modifier = Modifier.padding(start = Spacing.Sm))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (dontShow) scope.launch { AppPreferences(context).setSharingNoticeHidden(true) }
                onDismiss()
            }) { Text(appString(R.string.common_ok)) }
        }
    )
}
