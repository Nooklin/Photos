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
import org.nooklin.photos.R
import org.nooklin.photos.appString

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
                title = { Text(appString(R.string.common_hidden_image_sharing), fontWeight = FontWeight.Bold) },
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
                appString(R.string.common_hidden_image_sharing),
                listOf({
                    SettingsToggleRow(
                        appString(R.string.settings_hidden_sharing_allow_sharing_hidden_images),
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
                    appString(R.string.settings_hidden_sharing_sharing_is_turned_off_for),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            } else {
                Text(
                    appString(R.string.settings_hidden_sharing_when_off_the_default_the),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }
        }
    }

    if (showEnableConfirm) {
        ConfirmDeleteDialog(
            title = appString(R.string.settings_hidden_sharing_allow_sharing_hidden_images_2),
            text = appString(R.string.settings_hidden_sharing_hidden_photos_will_be_shareable),
            confirmLabel = appString(R.string.settings_hidden_sharing_allow_sharing),
            onConfirm = {
                showEnableConfirm = false
                onHiddenImageSharingEnabledChange(true)
            },
            onDismiss = { showEnableConfirm = false }
        )
    }
}
