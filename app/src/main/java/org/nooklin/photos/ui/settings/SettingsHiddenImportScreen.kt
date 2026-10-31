package org.nooklin.photos.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.ImportEntry
import org.nooklin.photos.data.ImportProgress
import org.nooklin.photos.data.ImportResult
import org.nooklin.photos.data.ImportSource
import androidx.compose.ui.semantics.Role
import org.nooklin.photos.ui.theme.Spacing
import org.nooklin.photos.ui.theme.AppText

// One row of the preview: an album (folder null) or a folder inside it
private class ImportRow(val album: String?, val folder: String?, val count: Int)

// Import a folder structure (or a zip made by the organized export) into the Hidden Section
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHiddenImportScreen(
    progress: ImportProgress?,
    onScan: (ImportSource, (List<ImportEntry>?, Set<String>) -> Unit) -> Unit,
    onImport: (ImportSource, List<ImportEntry>, Boolean, Boolean, (ImportResult?) -> Unit) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
    // false: the same screen imports into the normal library
    hidden: Boolean = true
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var sourceName by remember { mutableStateOf("") }
    var source by remember { mutableStateOf<ImportSource?>(null) }
    var entries by remember { mutableStateOf<List<ImportEntry>>(emptyList()) }
    var existing by remember { mutableStateOf<Set<String>>(emptySet()) }
    var scanning by remember { mutableStateOf(false) }
    var unreadable by remember { mutableStateOf(false) }
    // Keys of the unchecked rows: "album" or "album/folder", loose photos are "" (empty)
    var excluded by remember { mutableStateOf<Set<String>>(emptySet()) }
    var deleteAfter by remember { mutableStateOf(false) }
    var showConflict by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ImportResult?>(null) }
    var cancelled by remember { mutableStateOf(false) }
    var askDeleteZip by remember { mutableStateOf(false) }
    // null: not asked, true: zip deleted, false: it could not be deleted
    var zipDeleted by remember { mutableStateOf<Boolean?>(null) }

    fun scan(picked: Uri, isZip: Boolean) {
        val s = ImportSource(picked, isZip)
        source = s
        sourceName = try {
            if (isZip) {
                context.contentResolver.query(picked, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            } else {
                DocumentsContract.getTreeDocumentId(picked).substringAfterLast(':').substringAfterLast('/')
                    .ifBlank { DocumentsContract.getTreeDocumentId(picked) }
            }
        } catch (e: Exception) {
            null
        } ?: ""
        scanning = true
        unreadable = false
        result = null
        cancelled = false
        zipDeleted = null
        excluded = emptySet()
        deleteAfter = false
        onScan(s) { found, names ->
            scanning = false
            if (found == null) unreadable = true else entries = found
            existing = names
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scan(uri, false)
    }
    // Own intent: the zip may be deleted afterwards, so write access is asked too
    val zipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { activity ->
        activity.data?.data?.let { scan(it, true) }
    }

    fun key(e: ImportEntry) = e.album.orEmpty()
    fun included(e: ImportEntry): Boolean {
        val album = e.album.orEmpty()
        if (album in excluded) return false
        val folderKey = if (e.folder != null) "$album/${e.folder}" else null
        return folderKey == null || folderKey !in excluded
    }
    val selected = entries.filter { included(it) }

    // After a scan the list scrolls to the chosen source and the tree, so they are not below the screen
    LaunchedEffect(scanning, entries) {
        if (!scanning && entries.isNotEmpty()) {
            // Item 0 holds the intro and the buttons, the chosen source is the next item
            listState.animateScrollToItem(1)
        }
    }

    // The result (and the skipped list under it) is at the bottom, so it is scrolled into view
    LaunchedEffect(result) {
        if (result != null) listState.animateScrollToItem((listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
    }

    val rows = remember(entries) {
        val list = mutableListOf<ImportRow>()
        entries.groupBy { it.album }.toSortedMap(compareBy { it ?: "" }).forEach { (album, inAlbum) ->
            list.add(ImportRow(album, null, inAlbum.size))
            inAlbum.filter { it.folder != null }.groupBy { it.folder }.toSortedMap(compareBy { it ?: "" })
                .forEach { (folder, inFolder) -> list.add(ImportRow(album, folder, inFolder.size)) }
        }
        list
    }

    fun start(merge: Boolean) {
        val s = source ?: return
        showConflict = false
        cancelled = false
        onImport(s, selected, merge, deleteAfter && !s.isZip) { r ->
            result = r
            cancelled = r == null
            // Nothing failed, so the zip is no longer needed: ask first
            if (r != null && s.isZip && r.failed == 0) askDeleteZip = true
        }
    }

    val conflicts = selected.mapNotNull { it.album }.distinct().filter { it.lowercase() in existing }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.import_row), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = progress == null) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Text(
                    appString(if (hidden) R.string.import_row_info else R.string.import_row_info_library),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = Spacing.Lg)
                )
                Text(
                    appString(R.string.import_how_title),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    appString(R.string.import_how_steps),
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
                )
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { folderLauncher.launch(null) }, enabled = progress == null && !scanning) {
                        Text(appString(R.string.import_choose_folder))
                    }
                    OutlinedButton(
                        onClick = {
                            zipLauncher.launch(
                                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "application/*"
                                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/x-zip-compressed"))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                }
                            )
                        },
                        enabled = progress == null && !scanning
                    ) {
                        Text(appString(R.string.import_choose_zip))
                    }
                }
                Text(
                    appString(R.string.import_example_title),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = Spacing.Xl, bottom = 8.dp)
                )
                Text(
                    appString(R.string.import_example),
                    fontFamily = FontFamily.Monospace,
                    fontSize = AppText.Body,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(Spacing.Lg)
                )
                Text(
                    appString(if (hidden) R.string.import_rules else R.string.import_rules_library),
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = Spacing.Lg, bottom = 16.dp)
                )
            }
            if (scanning) {
                item {
                    Text(appString(R.string.import_scanning), modifier = Modifier.padding(16.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                }
            }
            if (unreadable) {
                item { Text(appString(R.string.import_unreadable), modifier = Modifier.padding(16.dp)) }
            }
            if (source != null && !scanning && !unreadable && entries.isEmpty()) {
                item { Text(appString(R.string.import_nothing), modifier = Modifier.padding(16.dp)) }
            }
            if (entries.isNotEmpty() && !scanning) {
                item(key = "chosen") {
                    Text(
                        appString(if (source?.isZip == true) R.string.import_selected_zip else R.string.import_selected_folder, sourceName),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    )
                }
                item {
                    Text(
                        appString(R.string.import_summary, entries.size, rows.count { it.folder == null && it.album != null }),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(rows) { row ->
                    val album = row.album.orEmpty()
                    val rowKey = if (row.folder == null) album else "$album/${row.folder}"
                    val albumOff = album in excluded
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = progress == null && (row.folder == null || !albumOff), role = Role.Button) {
                                excluded = if (rowKey in excluded) excluded - rowKey else excluded + rowKey
                            }
                            .padding(start = if (row.folder == null) 8.dp else 40.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rowKey !in excluded && !(row.folder != null && albumOff),
                            onCheckedChange = null,
                            enabled = progress == null && (row.folder == null || !albumOff),
                            modifier = Modifier.padding(12.dp)
                        )
                        Text(
                            when {
                                row.album == null -> appString(R.string.import_loose)
                                row.folder != null -> row.folder
                                else -> row.album
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Text(row.count.toString())
                    }
                }
                if (source?.isZip == false) {
                    item {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(appString(R.string.import_delete_after))
                                Text(appString(R.string.import_delete_after_info), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = deleteAfter, onCheckedChange = { deleteAfter = it }, enabled = progress == null)
                        }
                    }
                }
                item {
                    Button(
                        onClick = { if (conflicts.isNotEmpty()) showConflict = true else start(true) },
                        enabled = progress == null && selected.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text(appString(R.string.import_start, selected.size))
                    }
                }
            }
            if (progress != null) {
                item {
                    Column(Modifier.padding(16.dp)) {
                        if (progress.total > 0) {
                            LinearProgressIndicator(progress = { progress.done / progress.total.toFloat() }, modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                        Text(
                            if (progress.checking) appString(if (hidden) R.string.import_progress_checking else R.string.import_progress_checking_library, progress.done, progress.total)
                            else appString(R.string.import_progress_importing, progress.done, progress.total),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        TextButton(onClick = onCancel) { Text(appString(R.string.common_cancel)) }
                    }
                }
            }
            result?.let { r ->
                item {
                    Column(Modifier.padding(16.dp)) {
                        Text(if (r.imported == 0) appString(R.string.import_none, r.skipped.size, r.failed) else appString(R.string.import_done, r.imported, r.skipped.size, r.failed), fontWeight = FontWeight.Bold)
                        if (r.notDeleted > 0) Text(appString(R.string.import_not_deleted, r.notDeleted))
                        zipDeleted?.let { ok ->
                            Text(appString(if (ok) R.string.import_zip_deleted else R.string.import_zip_not_deleted))
                        }
                        if (r.skipped.isNotEmpty()) {
                            Text(appString(if (hidden) R.string.import_skipped_list else R.string.import_skipped_list_library), modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
                items(r.skipped) { name -> Text(name, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) }
            }
            if (cancelled) {
                item { Text(appString(R.string.import_cancelled), modifier = Modifier.padding(16.dp)) }
            }
        }
    }

    if (showConflict) {
        AlertDialog(
            onDismissRequest = { showConflict = false },
            title = { Text(appString(R.string.import_conflict_title)) },
            text = { Text(appString(if (hidden) R.string.import_conflict_text else R.string.import_conflict_text_library, conflicts.joinToString(", "))) },
            confirmButton = { TextButton(onClick = { start(true) }) { Text(appString(R.string.import_merge)) } },
            dismissButton = {
                Row {
                    TextButton(onClick = { showConflict = false }) { Text(appString(R.string.common_cancel)) }
                    TextButton(onClick = { start(false) }) { Text(appString(R.string.import_new_name)) }
                }
            }
        )
    }

    if (askDeleteZip) {
        val zipUri = source?.uri
        val zipName = remember(zipUri) {
            try {
                zipUri?.let { u ->
                    context.contentResolver.query(u, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            val size = if (c.isNull(1)) null else android.text.format.Formatter.formatShortFileSize(context, c.getLong(1))
                            listOfNotNull(c.getString(0), size).joinToString(", ")
                        } else null
                    }
                }
            } catch (e: Exception) {
                null
            } ?: ""
        }
        AlertDialog(
            onDismissRequest = { askDeleteZip = false },
            title = { Text(appString(R.string.import_zip_delete_title)) },
            text = { Text(appString(if (hidden) R.string.import_zip_delete_text else R.string.import_zip_delete_text_library, zipName)) },
            confirmButton = {
                TextButton(onClick = {
                    askDeleteZip = false
                    zipDeleted = try {
                        zipUri != null && DocumentsContract.deleteDocument(context.contentResolver, zipUri)
                    } catch (e: Exception) {
                        false
                    }
                }) { Text(appString(R.string.import_zip_delete)) }
            },
            dismissButton = { TextButton(onClick = { askDeleteZip = false }) { Text(appString(R.string.import_zip_keep)) } }
        )
    }
}
