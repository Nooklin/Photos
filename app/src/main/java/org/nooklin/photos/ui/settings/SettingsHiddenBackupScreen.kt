package org.nooklin.photos.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nooklin.photos.ui.hidden.HiddenExportProgress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Hidden files live in app-private storage, which Android deletes on uninstall. This export is the only way to keep a copy.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHiddenBackupScreen(
    progress: HiddenExportProgress?,
    onExportFlat: (Uri, (Boolean) -> Unit) -> Unit,
    onExportOrganized: (Uri, (Boolean) -> Unit) -> Unit,
    onNavigateToSelectExport: () -> Unit,
    onBack: () -> Unit
) {
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val timestamp = remember {
        SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
    }

    val flatLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        statusMessage = null
        onExportFlat(uri) { success ->
            statusMessage = if (success) "Export complete." else "Export failed. Nothing was overwritten."
        }
    }

    val organizedLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        statusMessage = null
        onExportOrganized(uri) { success ->
            statusMessage = if (success) "Export complete." else "Export failed. Nothing was overwritten."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidden Photos Backup", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Text(
                "Photos are stored in this app's private storage, and are permanently " +
                    "deleted if this app is uninstalled, that's what keeps them hidden from " +
                    "every other app, but it also means nothing preserves them automatically. " +
                    "Export a copy here before uninstalling or switching devices.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            SettingsGroup(
                "Export All",
                listOf({
                    SettingsNavRow("Export All Hidden Photos (.zip)", Icons.Filled.FolderZip) {
                        statusMessage = null
                        flatLauncher.launch("nooklin-hidden-backup-$timestamp.zip")
                    }
                })
            )
            Text(
                "The simplest, fastest option: every hidden photo goes into one flat zip file, ready to unpack anywhere.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SettingsGroup(
                "Export Organized by Album",
                listOf({
                    SettingsNavRow("Export Organized by Album (.zip)", Icons.Filled.Photo) {
                        statusMessage = null
                        organizedLauncher.launch("nooklin-hidden-backup-organized-$timestamp.zip")
                    }
                })
            )
            Text(
                "Keeps which Hidden Album each photo was in, organized into folders inside the zip. Takes a little longer to build than a flat export.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SettingsGroup(
                "Export Selected Photos",
                listOf({
                    SettingsNavRow("Export Selected Photos (.zip)", Icons.Filled.Checklist) {
                        statusMessage = null
                        onNavigateToSelectExport()
                    }
                })
            )
            Text(
                "Pick a specific subset of hidden photos to export, rather than everything at once.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (progress != null) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (progress.total > 0) {
                        LinearProgressIndicator(
                            progress = { progress.done / progress.total.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Exporting ${progress.done} of ${progress.total}…",
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        // Indeterminate until the file list is known
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            statusMessage?.let { message ->
                Text(message, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            }
        }
    }
}
