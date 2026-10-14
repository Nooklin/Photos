package org.nooklin.photos.ui.settings

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
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
import org.nooklin.photos.data.local.ShareStripOptions
import org.nooklin.photos.data.local.StripCategory
import org.nooklin.photos.ui.common.ConfirmDeleteDialog
import org.nooklin.photos.R
import org.nooklin.photos.appString

// Turning sharing back on is confirmed first, turning it off is the protective direction
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsExifScreen(
    removeExifOnShare: Boolean,
    onRemoveExifOnShareChange: (Boolean) -> Unit,
    stripOptions: ShareStripOptions,
    onStripOptionChange: (StripCategory, Boolean) -> Unit,
    sharingDisabled: Boolean,
    onSharingDisabledChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var showEnableSharingConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_sharing), fontWeight = FontWeight.Bold) },
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
                appString(R.string.common_sharing),
                listOf({
                    SettingsToggleRow(
                        appString(R.string.settings_exif_disable_sharing),
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
                appString(R.string.settings_exif_on_by_default_turns_off),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )

            SettingsGroup(
                appString(R.string.settings_exif_exif_data),
                listOf({ SettingsToggleRow(appString(R.string.settings_exif_remove_exif_data_when_sharing), removeExifOnShare, Icons.Filled.LocationOff, onCheckedChange = onRemoveExifOnShareChange) })
            )
            Text(
                appString(R.string.settings_exif_when_on_the_default_sharing),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )

            // What is removed, only while Remove Exif data is on
            if (removeExifOnShare) {
                SettingsGroup(
                    appString(R.string.settings_exif_what_to_remove),
                    listOf(
                        { SettingsToggleRow(appString(R.string.settings_exif_strip_location), stripOptions.location, Icons.Filled.LocationOn, onCheckedChange = { onStripOptionChange(StripCategory.LOCATION, it) }) },
                        { SettingsToggleRow(appString(R.string.settings_exif_strip_camera), stripOptions.camera, Icons.Filled.CameraAlt, onCheckedChange = { onStripOptionChange(StripCategory.CAMERA, it) }) },
                        { SettingsToggleRow(appString(R.string.settings_exif_strip_date_time), stripOptions.dateTime, Icons.Filled.Schedule, onCheckedChange = { onStripOptionChange(StripCategory.DATE_TIME, it) }) },
                        { SettingsToggleRow(appString(R.string.settings_exif_strip_description), stripOptions.description, Icons.Filled.Description, onCheckedChange = { onStripOptionChange(StripCategory.DESCRIPTION, it) }) }
                    )
                )
                Text(
                    appString(R.string.settings_exif_what_to_remove_help),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }
        }
    }

    if (showEnableSharingConfirm) {
        ConfirmDeleteDialog(
            title = appString(R.string.settings_exif_enable_sharing),
            text = appString(R.string.settings_exif_this_turns_sharing_back_on),
            confirmLabel = appString(R.string.settings_exif_enable_sharing_2),
            onConfirm = {
                showEnableSharingConfirm = false
                onSharingDisabledChange(false)
            },
            onDismiss = { showEnableSharingConfirm = false }
        )
    }
}
