package org.nooklin.photos.ui.settings

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOff
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

// Turning sharing back on is confirmed first, turning it off is the protective direction
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsExifScreen(
    removeExifOnShare: Boolean,
    onRemoveExifOnShareChange: (Boolean) -> Unit,
    sharingDisabled: Boolean,
    onSharingDisabledChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var showEnableSharingConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sharing", fontWeight = FontWeight.Bold) },
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
                "Sharing",
                listOf({
                    SettingsToggleRow(
                        "Disable Sharing",
                        sharingDisabled,
                        Icons.Filled.Share,
                        onCheckedChange = { turningOn ->
                            if (!turningOn) {
                                showEnableSharingConfirm = true
                            } else {
                                onSharingDisabledChange(true)
                            }
                        }
                    )
                })
            )
            Text(
                "On by default. Turns off the ability to share photos out of this app entirely, keeping Nooklin Photos a closed, private space with no way to send anything elsewhere. Turn this off if you want to be able to share photos.",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )

            SettingsGroup(
                "Exif Data",
                listOf({ SettingsToggleRow("Remove Exif Data When Sharing", removeExifOnShare, Icons.Filled.LocationOff, onCheckedChange = onRemoveExifOnShareChange) })
            )
            Text(
                "When on (the default), sharing a photo strips its Exif data, including GPS location, camera make/model, and capture settings, before handing it to another app. When off, the original file is shared as-is, Exif data intact.",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )
        }
    }

    if (showEnableSharingConfirm) {
        ConfirmDeleteDialog(
            title = "Enable Sharing?",
            text = "This turns sharing back on for the whole app. Photos will be shareable to other apps again.",
            confirmLabel = "Enable Sharing",
            onConfirm = {
                showEnableSharingConfirm = false
                onSharingDisabledChange(false)
            },
            onDismiss = { showEnableSharingConfirm = false }
        )
    }
}
