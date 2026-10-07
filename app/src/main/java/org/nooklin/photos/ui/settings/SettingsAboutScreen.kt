package org.nooklin.photos.ui.settings

import androidx.compose.ui.text.font.FontWeight
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
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

// GitHub URL confirmed by the owner, not separately verified
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.Bold) },
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
                "About",
                listOf(
                    {
                        SettingsNavRow("GitHub", Icons.Filled.Code) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Nooklin/Photos")))
                            } catch (e: ActivityNotFoundException) {
                                // No browser installed, nothing to open.
                            }
                        }
                    },
                    { SettingsNavRow("Privacy", Icons.Filled.PrivacyTip) { showPrivacy = true } },
                    { SettingsNavRow("Terms", Icons.Filled.Description) { showTerms = true } }
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
            title = { Text("Privacy") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Everything Nooklin Photos does happens on this device. There is no " +
                            "server, no account, and no internet permission requested by this app " +
                            "at all, it is not technically able to send anything anywhere, even if " +
                            "it wanted to."
                    )
                    Text(
                        "No analytics, no crash reporting, no usage tracking, no telemetry of any " +
                            "kind. This app does not know, and cannot find out, how you use it.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "Machine Learning (face grouping, pet and object detection) runs entirely " +
                            "on your device using bundled models. Photos and any results are never " +
                            "uploaded anywhere.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "Hidden items are moved into this app's own private storage, invisible to " +
                            "the system photo index and every other app on your phone. Exporting a " +
                            "Hidden Photos Backup, or sharing any photo, only happens when you " +
                            "explicitly choose to, nothing is ever sent out automatically.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "The Hidden Section is locked with your device's biometric or screen lock, " +
                            "and optionally an extra PIN. Hidden files are protected by that lock and " +
                            "by the app's private storage, they are not encrypted.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "Edited photos and clips saved as new copies are new files on your device, " +
                            "and a trim you save on a video is stored only in this app's settings on " +
                            "this device, the video file itself is not changed.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "The one notification this app can show (library scan complete) is " +
                            "generated and displayed entirely on your device, it is not a push " +
                            "notification and involves no server.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) { Text("OK") }
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
            title = { Text("Terms") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Nooklin Photos is provided free of charge, and \"as is,\" without warranty " +
                            "of any kind, express or implied, including, without limitation, any " +
                            "warranty that it will work correctly, be free of bugs, or be fit for " +
                            "any particular purpose."
                    )
                    Text(
                        "To the fullest extent permitted by law, the developers of this app accept " +
                            "no liability for any loss or damage arising from its use, including but " +
                            "not limited to loss of photos, videos, or other data, however that loss " +
                            "occurs.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "Data retention and backup are entirely your responsibility. Photos and " +
                            "videos you add to your device's regular library remain managed by " +
                            "Android's own MediaStore, as with any gallery app. Photos moved into " +
                            "this app's Hidden Section live in this app's own private storage, " +
                            "which is permanently deleted if the app is uninstalled, this app " +
                            "provides an export/backup tool for exactly this reason, and using it, " +
                            "on your own schedule, is on you.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "Photos and videos you delete stay in Recently Deleted for 30 days and are " +
                            "then removed permanently. After that they cannot be restored by this app.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "By using this app, you accept these terms and agree that you are solely " +
                            "responsible for your own data, backups, and how you use this app.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "This text is provided as a general disclaimer and has not been reviewed " +
                            "by a lawyer for any specific jurisdiction.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTerms = false }) { Text("OK") }
            }
        )
    }
}
