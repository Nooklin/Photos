package org.nooklin.photos.ui.gallery

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.drawable.toBitmap
import androidx.print.PrintHelper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.createShareUris
import org.nooklin.photos.data.saveToFolder
import org.nooklin.photos.ui.common.freshImageRequest
import org.nooklin.photos.ui.common.GlassPill

// Share sheet for the Library selection: share, save to a folder, export the originals, copy
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSelectionSheet(
    selectedItems: List<MediaItem>,
    removeMetadataDefault: Boolean,
    onDismiss: () -> Unit,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var included by remember { mutableStateOf(selectedItems.map { it.id }.toSet()) }
    // Only for this share, starts at the Settings, Sharing value
    var removeMetadata by remember { mutableStateOf(removeMetadataDefault) }
    var busy by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    // Set when the folder picker is open: true exports the originals, false follows the metadata switch
    var exportOriginals by remember { mutableStateOf<Boolean?>(null) }
    val chosen = selectedItems.filter { it.id in included }
    val canAct = chosen.isNotEmpty() && !busy

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    // Stops the work in progress: nothing is shared, saved or copied afterwards
    fun cancelWork() {
        job?.cancel()
        job = null
        busy = false
    }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        val originals = exportOriginals
        exportOriginals = null
        if (treeUri != null && originals != null) {
            busy = true
            job = scope.launch {
                val result = withContext(Dispatchers.IO) {
                    val running = coroutineContext[Job]
                    val sources = if (originals) {
                        chosen.map { it to it.uri }
                    } else {
                        createShareUris(context, chosen, removeMetadata) { running?.isActive == false }
                    }
                    saveToFolder(context, treeUri, sources)
                }
                busy = false
                job = null
                if (result.saved > 0) {
                    toast(appString(R.string.share_sheet_saved, result.saved))
                } else {
                    toast(appString(R.string.share_sheet_nothing_saved))
                }
                if (result.skipped > 0) toast(appString(R.string.share_sheet_skipped, result.skipped))
                if (result.saved > 0) onFinished()
            }
        }
    }

    // What happens with the prepared files: the system share menu, copy, print, wallpaper or one chosen app
    fun startShare(mode: ShareMode, app: ShareApp? = null) {
        busy = true
        job = scope.launch {
            val pairs = withContext(Dispatchers.IO) {
                val running = coroutineContext[Job]
                createShareUris(context, chosen, removeMetadata) { running?.isActive == false }
            }
            busy = false
            job = null
            val uris = pairs.mapNotNull { it.second }
            val skipped = pairs.size - uris.size
            if (skipped > 0) toast(appString(R.string.share_sheet_skipped, skipped))
            if (uris.isEmpty()) return@launch
            val clip = ClipData.newRawUri("Nooklin Photos", uris.first())
            uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            val usedItems = pairs.filter { it.second != null }.map { it.first }
            when (mode) {
                ShareMode.COPY -> {
                    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
                    toast(appString(R.string.share_sheet_copied, uris.size))
                }
                ShareMode.PRINT -> {
                    try {
                        PrintHelper(context).apply { scaleMode = PrintHelper.SCALE_MODE_FIT }
                            .printBitmap("Nooklin Photos", uris.first())
                    } catch (e: Exception) {
                        toast(appString(R.string.share_sheet_print_failed))
                        return@launch
                    }
                }
                ShareMode.WALLPAPER -> {
                    val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                        setDataAndType(uris.first(), "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, appString(R.string.share_sheet_wallpaper)))
                }
                ShareMode.SHARE, ShareMode.APP -> {
                    val intent = if (uris.size == 1) {
                        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
                    } else {
                        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    }
                    intent.type = shareMimeType(usedItems)
                    intent.clipData = clip
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (mode == ShareMode.APP && app != null) {
                        intent.setClassName(app.packageName, app.activityName)
                        context.startActivity(intent)
                    } else {
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                }
            }
            onFinished()
        }
    }

    // Apps that accept a share (row on top), found with a package query declared in the manifest
    var apps by remember { mutableStateOf<List<ShareApp>>(emptyList()) }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { findShareApps(context, shareMimeType(selectedItems)) }
    }
    // Print and wallpaper work on one photo, not on videos or several files
    val single = chosen.size == 1 && !chosen[0].isVideo

    ModalBottomSheet(onDismissRequest = {
        cancelWork()
        onDismiss()
    }) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    appString(R.string.share_sheet_selected, chosen.size),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    cancelWork()
                    onDismiss()
                }) {
                    Icon(Icons.Filled.Close, contentDescription = appString(R.string.common_cancel))
                }
            }

            ShareBlock {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(appString(R.string.share_sheet_remove_metadata), modifier = Modifier.weight(1f))
                    Switch(checked = removeMetadata, onCheckedChange = { removeMetadata = it }, enabled = !busy)
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(selectedItems, key = { it.id }) { item ->
                    val isIncluded = item.id in included
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !busy) {
                                included = if (isIncluded) included - item.id else included + item.id
                            }
                    ) {
                        AsyncImage(
                            model = freshImageRequest(context, item.uri, item.dateModified),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(72.dp).alpha(if (isIncluded) 1f else 0.35f)
                        )
                        if (isIncluded) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = appString(R.string.common_selected),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(20.dp)
                            )
                        }
                    }
                }
            }

            if (apps.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(apps, key = { it.packageName + it.activityName }) { app ->
                        Column(
                            Modifier
                                .width(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = canAct) { startShare(ShareMode.APP, app) }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).alpha(if (canAct) 1f else 0.4f)
                            )
                            Text(
                                app.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            ShareBlock {
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(appString(R.string.share_sheet_preparing), modifier = Modifier.weight(1f))
                        TextButton(onClick = { cancelWork() }) { Text(appString(R.string.common_cancel)) }
                    }
                }
                ShareActionRow(Icons.Filled.IosShare, appString(R.string.common_share), canAct) { startShare(ShareMode.SHARE) }
                HorizontalDivider()
                ShareActionRow(Icons.Filled.SaveAlt, appString(R.string.share_sheet_save_to_folder), canAct) {
                    exportOriginals = false
                    folderLauncher.launch(null)
                }
                HorizontalDivider()
                ShareActionRow(Icons.Filled.Folder, appString(R.string.share_sheet_export_original), canAct) {
                    exportOriginals = true
                    folderLauncher.launch(null)
                }
                HorizontalDivider()
                ShareActionRow(Icons.Filled.ContentCopy, appString(R.string.share_sheet_copy), canAct) { startShare(ShareMode.COPY) }
                if (single) {
                    HorizontalDivider()
                    ShareActionRow(Icons.Filled.Print, appString(R.string.share_sheet_print), canAct) { startShare(ShareMode.PRINT) }
                    HorizontalDivider()
                    ShareActionRow(Icons.Filled.Wallpaper, appString(R.string.share_sheet_wallpaper), canAct) { startShare(ShareMode.WALLPAPER) }
                }
            }
        }
    }
}

// Glass block like the Home sections
@Composable
private fun ShareBlock(content: @Composable () -> Unit) {
    Surface(
        color = GlassPill.adaptiveGlassBackground,
        shape = RoundedCornerShape(20.dp),
        border = GlassPill.adaptiveBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column { content() }
    }
}

@Composable
private fun ShareActionRow(icon: ImageVector, text: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.size(16.dp))
        Text(text)
    }
}

private enum class ShareMode { SHARE, COPY, PRINT, WALLPAPER, APP }

private class ShareApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.ImageBitmap
)

private fun shareMimeType(items: List<MediaItem>): String = when {
    items.isEmpty() || items.none { it.isVideo } -> "image/*"
    items.all { it.isVideo } -> "video/*"
    else -> "*/*"
}

// Needs the <queries> entry for ACTION_SEND in the manifest (no permission to list all apps)
private fun findShareApps(context: android.content.Context, mime: String): List<ShareApp> {
    val pm = context.packageManager
    // A mixed selection is looked up as images, the declared query types are image/* and video/*
    val queryType = if (mime == "*/*") "image/*" else mime
    val intent = Intent(Intent.ACTION_SEND).setType(queryType)
    return try {
        pm.queryIntentActivities(intent, android.content.pm.PackageManager.ResolveInfoFlags.of(0))
            .filter { it.activityInfo.packageName != context.packageName }
            .map { info ->
                ShareApp(
                    packageName = info.activityInfo.packageName,
                    activityName = info.activityInfo.name,
                    label = info.loadLabel(pm).toString(),
                    icon = info.loadIcon(pm).toBitmap(96, 96).asImageBitmap()
                )
            }
            .sortedBy { it.label.lowercase() }
    } catch (e: Exception) {
        emptyList()
    }
}
