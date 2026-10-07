package org.nooklin.photos.ui.settings

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.nooklin.photos.ui.common.ConfirmDeleteDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHiddenSharingScreen(
    hiddenImageSharingEnabled: Boolean,
    onHiddenImageSharingEnabledChange: (Boolean) -> Unit,
    sharingDisabled: Boolean,
    onBack: () -> Unit
) {
    var showEnableConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidden Image Sharing", fontWeight = FontWeight.Bold) },
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
                "Hidden Image Sharing",
                listOf({
                    SettingsToggleRow(
                        "Allow Sharing Hidden Images",
                        hiddenImageSharingEnabled,
                        Icons.Filled.Share,
                        enabled = !sharingDisabled,
                        onCheckedChange = { turningOn ->
                            if (turningOn) {
                                showEnableConfirm = true
                            } else {
                                onHiddenImageSharingEnabledChange(false)
                            }
                        }
                    )
                })
            )
            if (sharingDisabled) {
                Text(
                    "Sharing is turned off for the whole app (Settings > Sharing). Turn that back on first to change this setting.",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            } else {
                Text(
                    "When off (the default), the Hidden photo viewer has no Share button, so a hidden photo can't be shared out by mistake. Turning this on adds a Share button back to that viewer, same EXIF-stripped share behavior as your regular Library.",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }
        }
    }

    if (showEnableConfirm) {
        ConfirmDeleteDialog(
            title = "Allow Sharing Hidden Images?",
            text = "Hidden photos will be shareable to other apps again, using the same EXIF-stripped share behavior as your regular Library. Hidden photos still won't appear anywhere else on your device.",
            confirmLabel = "Allow Sharing",
            onConfirm = {
                showEnableConfirm = false
                onHiddenImageSharingEnabledChange(true)
            },
            onDismiss = { showEnableConfirm = false }
        )
    }
}
