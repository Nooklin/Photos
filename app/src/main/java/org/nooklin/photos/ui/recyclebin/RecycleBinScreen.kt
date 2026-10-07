package org.nooklin.photos.ui.recyclebin

import org.nooklin.photos.ui.common.scrollToItemCentered

import org.nooklin.photos.ui.common.ScrollToPendingZoomKey
import org.nooklin.photos.ui.common.zoomSource
import org.nooklin.photos.ui.common.zoomBackdropSource
import org.nooklin.photos.ui.common.freshImageRequest
import androidx.compose.foundation.lazy.grid.rememberLazyGridState

import androidx.compose.ui.text.font.FontWeight
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.nooklin.photos.data.local.LocalMetadataStore.RecycleBinEntry
import org.nooklin.photos.ui.common.GlassPill
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.nooklin.photos.data.local.MAX_GRID_COLUMNS
import org.nooklin.photos.data.local.MIN_GRID_COLUMNS
import org.nooklin.photos.ui.common.pinchToChangeGridColumns
import org.nooklin.photos.ui.common.rememberScreenGridColumns

private const val RETENTION_DAYS = 30
private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

fun daysRemaining(deletedAt: Long): Int {
    val elapsedDays = (System.currentTimeMillis() - deletedAt) / MILLIS_PER_DAY
    return (RETENTION_DAYS - elapsedDays).toInt().coerceAtLeast(0)
}

private fun isExpired(deletedAt: Long): Boolean = daysRemaining(deletedAt) <= 0

// Purging needs the system consent dialog, the files belong to MediaStore
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    entries: List<RecycleBinEntry>,
    gridColumns: Int,
    onRestore: (Long) -> Unit,
    onPurgeExpired: (List<Long>) -> Unit,
    onItemClick: (Int) -> Unit,
    onBack: () -> Unit,
    restoreConfirmTitle: String? = null,
    restoreConfirmText: ((count: Int) -> String)? = null
) {
    val context = LocalContext.current

    val purgeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        // Denied items fall back to the main gallery instead of staying in limbo
        onPurgeExpired(entries.filter { isExpired(it.deletedAt) }.map { it.mediaId })
    }

    val expired = entries.filter { isExpired(it.deletedAt) }
    LaunchedEffect(expired.map { it.mediaId }) {
        if (expired.isNotEmpty()) {
            val uris = expired.map { Uri.parse(it.uri) }
            val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
            purgeLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        }
    }

    var isSelecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var actionsMenuExpanded by remember { mutableStateOf(false) }
    var showBulkRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRecoverIds by remember { mutableStateOf(listOf<Long>()) }
    var pendingDeleteIds by remember { mutableStateOf(listOf<Long>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Only a confirmed delete is dropped, a cancelled one leaves everything as it was
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) onPurgeExpired(pendingDeleteIds)
        pendingDeleteIds = emptyList()
    }
    val screenColumns = rememberScreenGridColumns("recently_deleted", gridColumns)
    val cols = screenColumns.columns

    fun exitSelection() {
        isSelecting = false
        selectedIds = emptySet()
    }

    fun toggleSelect(mediaId: Long) {
        selectedIds = if (mediaId in selectedIds) selectedIds - mediaId else selectedIds + mediaId
    }

    fun performRecover(ids: List<Long>) {
        ids.forEach { onRestore(it) }
        exitSelection()
    }

    // Nothing selected means "All", also the items scrolled out of view
    fun targetIds(): List<Long> = if (selectedIds.isEmpty()) entries.map { it.mediaId } else entries.map { it.mediaId }.filter { it in selectedIds }

    val recycleGridState = rememberLazyGridState()
    ScrollToPendingZoomKey(
        lazyIndexOf = { key -> entries.indexOfFirst { it.mediaId == key }.takeIf { it >= 0 } },
        scrollToIndex = { i -> recycleGridState.scrollToItemCentered(i) }
    )

    Scaffold(
        modifier = Modifier.zoomBackdropSource(),
        topBar = {
            TopAppBar(
                title = { Text("Recently Deleted", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (entries.isNotEmpty()) {
                        if (isSelecting) {
                            Box {
                                CircleIconButton(
                                    icon = Icons.Filled.MoreVert,
                                    contentDescription = "More",
                                    onClick = { actionsMenuExpanded = true }
                                )
                                if (actionsMenuExpanded) {
                                    Popup(
                                        alignment = Alignment.TopEnd,
                                        onDismissRequest = { actionsMenuExpanded = false },
                                        properties = PopupProperties(focusable = true)
                                    ) {
                                        val all = selectedIds.isEmpty()
                                        Column(
                                            Modifier
                                                .clip(GlassPill.shape)
                                                .background(GlassPill.adaptivePopupBackground)
                                                .border(GlassPill.adaptiveBorder, GlassPill.shape)
                                                .padding(vertical = 8.dp)
                                        ) {
                                            PillAction(Icons.Filled.Delete, if (all) "Delete All" else "Delete") {
                                                actionsMenuExpanded = false
                                                pendingDeleteIds = targetIds()
                                                showDeleteConfirm = true
                                            }
                                            PillAction(Icons.Filled.Restore, if (all) "Recover All" else "Recover") {
                                                actionsMenuExpanded = false
                                                pendingRecoverIds = targetIds()
                                                if (restoreConfirmTitle != null && restoreConfirmText != null) {
                                                    showBulkRestoreConfirm = true
                                                } else {
                                                    performRecover(pendingRecoverIds)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            CircleIconButton(
                                icon = Icons.Filled.Close,
                                contentDescription = "Cancel selection",
                                onClick = { exitSelection() }
                            )
                        } else {
                            PillButton(text = "Select", onClick = { isSelecting = true })
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Recently Deleted is empty")
            }
            return@Scaffold
        }

        LazyVerticalGrid(
            state = recycleGridState,
            columns = GridCells.Fixed(cols),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(padding).pinchToChangeGridColumns(!isSelecting, cols, MIN_GRID_COLUMNS, MAX_GRID_COLUMNS, screenColumns.setColumns)
        ) {
            items(entries.size, key = { entries[it].mediaId }) { index ->
                val entry = entries[index]
                Box(
                    Modifier
                        // One column keeps each photo's own shape.
                        .aspectRatio(
                            if (cols == 1 && entry.width != null && entry.height != null && entry.height > 0) {
                                entry.width.toFloat() / entry.height.toFloat()
                            } else 1f
                        )
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .zoomSource(entry.mediaId)
                        .clickable {
                            if (isSelecting) toggleSelect(entry.mediaId) else onItemClick(index)
                        }
                ) {
                    AsyncImage(
                        model = freshImageRequest(LocalContext.current, entry.uri, entry.deletedAt),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        "${daysRemaining(entry.deletedAt)} days",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(4.dp)
                    )
                    if (isSelecting && entry.mediaId in selectedIds) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        val count = pendingDeleteIds.size
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(if (count == 1) "Delete 1 item?" else "Delete $count items?") },
            text = { Text("This permanently deletes them from your phone. It can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    val uris = entries.filter { it.mediaId in pendingDeleteIds }.map { Uri.parse(it.uri) }
                    if (uris.isNotEmpty()) {
                        val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                        deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                    }
                }) {
                    Text("Delete", color = Color(0xFFFF3B30))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showBulkRestoreConfirm && restoreConfirmTitle != null && restoreConfirmText != null) {
        AlertDialog(
            onDismissRequest = { showBulkRestoreConfirm = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(restoreConfirmTitle) },
            text = { Text(restoreConfirmText(pendingRecoverIds.size)) },
            confirmButton = {
                TextButton(onClick = {
                    showBulkRestoreConfirm = false
                    performRecover(pendingRecoverIds)
                }) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkRestoreConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(4.dp)
            .clip(GlassPill.shape)
            .background(GlassPill.adaptiveGlassBackground)
            .border(GlassPill.adaptiveBorder, GlassPill.shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .padding(4.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(GlassPill.adaptiveGlassBackground)
            .border(GlassPill.adaptiveBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
private fun PillAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .widthIn(min = 180.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Text(label, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 12.dp))
    }
}

