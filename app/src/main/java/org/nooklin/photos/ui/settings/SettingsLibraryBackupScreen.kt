package org.nooklin.photos.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.ui.hidden.HiddenExportProgress
import org.nooklin.photos.ui.theme.Spacing

// Backup of the normal library as a zip: everything flat, or organized by album and folder
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLibraryBackupScreen(
    progress: HiddenExportProgress?,
    onExport: (Uri, Boolean, (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val timestamp = remember { SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date()) }

    fun launcherResult(organized: Boolean): (Uri?) -> Unit = { uri ->
        if (uri != null) {
            statusMessage = null
            onExport(uri, organized) { success ->
                statusMessage = if (success) {
                    appString(R.string.settings_hidden_backup_export_complete)
                } else {
                    appString(R.string.settings_hidden_backup_export_failed_nothing_was_overwritten)
                }
            }
        }
    }
    val flatLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"), launcherResult(false)
    )
    val organizedLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"), launcherResult(true)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.library_backup_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = progress == null) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Text(
                appString(R.string.library_backup_intro),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = Spacing.Lg)
            )
            BackupExplanation(
                stepsText = appString(R.string.library_backup_steps),
                exampleTitle = appString(R.string.library_backup_example_title),
                example = appString(R.string.library_backup_example)
            )
            SettingsGroup(
                appString(R.string.library_backup_flat),
                listOf({
                    SettingsNavRow(appString(R.string.library_backup_flat), Icons.Filled.FolderZip) {
                        statusMessage = null
                        flatLauncher.launch("nooklin-library-backup-$timestamp.zip")
                    }
                })
            )
            Text(
                appString(R.string.library_backup_flat_info),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = Spacing.Lg)
            )
            SettingsGroup(
                appString(R.string.library_backup_organized),
                listOf({
                    SettingsNavRow(appString(R.string.library_backup_organized), Icons.Filled.Photo) {
                        statusMessage = null
                        organizedLauncher.launch("nooklin-library-backup-organized-$timestamp.zip")
                    }
                })
            )
            Text(
                appString(R.string.library_backup_organized_info),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = Spacing.Lg)
            )
            if (progress != null) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (progress.total > 0) {
                        LinearProgressIndicator(
                            progress = { progress.done / progress.total.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            appString(R.string.settings_hidden_backup_exporting_of, progress.done, progress.total),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
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
