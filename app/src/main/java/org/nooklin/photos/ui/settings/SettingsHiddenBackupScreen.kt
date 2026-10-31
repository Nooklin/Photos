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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nooklin.photos.ui.hidden.HiddenExportProgress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.ui.theme.Spacing
import org.nooklin.photos.ui.theme.AppText

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
            statusMessage = if (success) appString(R.string.settings_hidden_backup_export_complete) else appString(R.string.settings_hidden_backup_export_failed_nothing_was_overwritten)
        }
    }

    val organizedLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        statusMessage = null
        onExportOrganized(uri) { success ->
            statusMessage = if (success) appString(R.string.settings_hidden_backup_export_complete) else appString(R.string.settings_hidden_backup_export_failed_nothing_was_overwritten)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_hidden_photos_backup), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Text(
                appString(R.string.settings_hidden_backup_photos_are_stored_in_this),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = Spacing.Lg)
            )
            BackupExplanation(
                stepsText = appString(R.string.backup_how_steps),
                exampleTitle = appString(R.string.backup_example_title),
                example = appString(R.string.backup_example)
            )
            SettingsGroup(
                appString(R.string.settings_hidden_backup_export_all),
                listOf({
                    SettingsNavRow(appString(R.string.settings_hidden_backup_export_all_hidden_photos_zip), Icons.Filled.FolderZip) {
                        statusMessage = null
                        flatLauncher.launch("nooklin-hidden-backup-$timestamp.zip")
                    }
                })
            )
            Text(
                appString(R.string.settings_hidden_backup_the_simplest_fastest_option_every),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = Spacing.Lg)
            )

            SettingsGroup(
                appString(R.string.settings_hidden_backup_export_organized_by_album),
                listOf({
                    SettingsNavRow(appString(R.string.settings_hidden_backup_export_organized_by_album_zip), Icons.Filled.Photo) {
                        statusMessage = null
                        organizedLauncher.launch("nooklin-hidden-backup-organized-$timestamp.zip")
                    }
                })
            )
            Text(
                appString(R.string.settings_hidden_backup_keeps_which_hidden_album_each),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = Spacing.Lg)
            )

            SettingsGroup(
                appString(R.string.settings_hidden_backup_export_selected_photos),
                listOf({
                    SettingsNavRow(appString(R.string.settings_hidden_backup_export_selected_photos_zip), Icons.Filled.Checklist) {
                        statusMessage = null
                        onNavigateToSelectExport()
                    }
                })
            )
            Text(
                appString(R.string.settings_hidden_backup_pick_a_specific_subset_of),
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

// Same layout as the import screen: a short list of steps, then an example of what the zip looks like
@Composable
fun BackupExplanation(stepsText: String, exampleTitle: String, example: String) {
    Text(
        appString(R.string.backup_how_title),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    Text(
        stepsText,
        lineHeight = 24.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
    )
    Text(
        exampleTitle,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
    )
    Text(
        example,
        fontFamily = FontFamily.Monospace,
        fontSize = AppText.Body,
        lineHeight = 22.sp,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(Spacing.Lg)
    )
    Spacer(Modifier.height(28.dp))
}
