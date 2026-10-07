package org.nooklin.photos.ui.settings

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHiddenSectionScreen(
    hiddenSectionEnabled: Boolean,
    onHiddenSectionEnabledChange: (Boolean) -> Unit,
    hiddenPinSet: Boolean,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidden Section", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SettingsGroup(
                "Hidden Section",
                listOf({ 
                    SettingsToggleRow("Enable Hidden Section", hiddenSectionEnabled, Icons.Filled.Lock, onCheckedChange = onHiddenSectionEnabledChange) 
                })
            )
            if (hiddenSectionEnabled) {
                Text(
                    "When disabling Hidden Section, all data in the Hidden Section will remain there. Nothing is deleted and will be visible again after enabling Hidden Section again.",
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
                )
                SettingsGroup(
                    "Hidden Section Settings",
                    listOf(
                        { SettingsNavRow("Hidden Albums", Icons.Filled.PhotoAlbum) { onNavigate("hiddenalbums") } },
                        { SettingsNavRow("Hidden Image Sharing", Icons.Filled.Share) { onNavigate("hiddensharing") } },
                        {
                            SettingsNavRow(
                                if (hiddenPinSet) "Extra PIN (On)" else "Extra PIN (Off)",
                                Icons.Filled.Password
                            ) { onNavigate("hiddensection/hidden_pin") }
                        },
                        { SettingsNavRow("Hidden Photos Backup", Icons.Filled.CloudUpload) { onNavigate("hiddenbackup") } }
                    )
                )
            }
        }
    }
}
