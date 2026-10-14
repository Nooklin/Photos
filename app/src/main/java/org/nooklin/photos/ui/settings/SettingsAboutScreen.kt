package org.nooklin.photos.ui.settings

import androidx.compose.ui.text.font.FontWeight
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.BuildConfig
import org.nooklin.photos.R
import org.nooklin.photos.appString

// GitHub URL confirmed by the owner, not separately verified
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAboutScreen(onOpenChangelog: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_about), fontWeight = FontWeight.Bold) },
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
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(appString(R.string.settings_about_version))
                            Text(BuildConfig.VERSION_NAME)
                        }
                    },
                    { SettingsNavRow(appString(R.string.settings_about_changelog), Icons.Filled.History) { onOpenChangelog() } }
                )
            )
            Spacer(Modifier.height(12.dp))
            SettingsGroup(
                null,
                listOf(
                    {
                        SettingsNavRow(appString(R.string.settings_about_github), Icons.Filled.Code) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Nooklin/Photos")))
                            } catch (e: ActivityNotFoundException) {
                                // No browser installed, nothing to open.
                            }
                        }
                    },
                    { SettingsNavRow(appString(R.string.common_privacy), Icons.Filled.PrivacyTip) { showPrivacy = true } },
                    { SettingsNavRow(appString(R.string.settings_about_terms), Icons.Filled.Description) { showTerms = true } }
                )
            )
        }
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(R.string.common_privacy)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        appString(R.string.settings_about_everything_nooklin_photos_does_happens)
                    )
                    Text(
                        appString(R.string.settings_about_no_analytics_no_crash_reporting),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        appString(R.string.settings_about_machine_learning_face_grouping_pet),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        appString(R.string.settings_about_hidden_items_are_moved_into),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        appString(R.string.settings_about_the_hidden_section_is_locked),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        appString(R.string.settings_about_edited_photos_and_clips_saved),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        appString(R.string.settings_about_the_one_notification_this_app),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) { Text(appString(R.string.common_ok)) }
            }
        )
    }

    if (showTerms) {
        AlertDialog(
            onDismissRequest = { showTerms = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(appString(R.string.settings_about_terms)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        appString(R.string.settings_about_nooklin_photos_is_provided_free)
                    )
                    Text(
                        appString(R.string.settings_about_to_the_fullest_extent_permitted),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        appString(R.string.settings_about_data_retention_and_backup_are),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        appString(R.string.settings_about_photos_and_videos_you_delete),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        appString(R.string.settings_about_by_using_this_app_you),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        appString(R.string.settings_about_this_text_is_provided_as),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTerms = false }) { Text(appString(R.string.common_ok)) }
            }
        )
    }
}
