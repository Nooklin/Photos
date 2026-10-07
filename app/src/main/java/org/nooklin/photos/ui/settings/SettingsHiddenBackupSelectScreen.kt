package org.nooklin.photos.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.nooklin.photos.data.HiddenItem
import org.nooklin.photos.ui.common.SelectionGridPickerScreen
import org.nooklin.photos.ui.common.freshImageRequest

@Composable
fun SettingsHiddenBackupSelectScreen(
    items: List<HiddenItem>,
    gridColumns: Int,
    onExportSelected: (fileNames: Set<String>, destination: Uri, onResult: (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var pendingSelection by remember { mutableStateOf(setOf<String>()) }
    val timestamp = remember { SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            onExportSelected(pendingSelection, uri) { onBack() }
        } else {
            onBack()
        }
    }

    SelectionGridPickerScreen(
        title = "Select Photos to Export",
        items = items,
        key = { it.file.name },
        thumbnailModel = { freshImageRequest(context, it.thumbnailFile, it.thumbnailFile.lastModified()) },
        excludeKeys = emptySet(),
        gridColumns = gridColumns,
        emptyMessage = "No hidden photos to select",
        onConfirm = { selectedFileNames ->
            pendingSelection = selectedFileNames.toSet()
            launcher.launch("nooklin-hidden-backup-selected-$timestamp.zip")
        },
        onBack = onBack
    )
}
