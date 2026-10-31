package org.nooklin.photos.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.nooklin.photos.R
import org.nooklin.photos.appString

// One entry in Settings for the library: make a backup zip, or import a folder structure
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupImportScreen(onNavigate: (String) -> Unit, onBack: () -> Unit, hidden: Boolean = false) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.settings_backup_import_row), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SettingsGroup(
                null,
                listOf(
                    {
                        SettingsNavRow(
                            appString(if (hidden) R.string.common_hidden_photos_backup else R.string.settings_library_backup_row),
                            Icons.Filled.CloudUpload
                        ) { onNavigate(if (hidden) "hiddenbackup" else "librarybackup") }
                    },
                    { SettingsNavRow(appString(R.string.import_row), Icons.Filled.FolderZip) { onNavigate(if (hidden) "hiddenimport" else "libraryimport") } }
                )
            )
        }
    }
}
