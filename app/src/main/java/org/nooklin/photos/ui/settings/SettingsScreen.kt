package org.nooklin.photos.ui.settings

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.R
import org.nooklin.photos.appString
import androidx.compose.ui.semantics.Role
import org.nooklin.photos.ui.theme.Spacing
import org.nooklin.photos.ui.theme.AppText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.common_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SettingsHeading(appString(R.string.settings_head_general))
            SettingsGroup(
                null,
                listOf(
                    { SettingsNavRow(appString(R.string.common_appearance), Icons.Filled.Palette) { onNavigate("appearance") } },
                    { SettingsNavRow(appString(R.string.common_gallery), Icons.Filled.PhotoLibrary) { onNavigate("gallery") } },
                    { SettingsNavRow(appString(R.string.common_notifications), Icons.Filled.Notifications) { onNavigate("notifications") } }
                )
            )
            SettingsHeading(appString(R.string.settings_head_library))
            SettingsGroup(
                null,
                listOf(
                    { SettingsNavRow(appString(R.string.settings_backup_import_row), Icons.Filled.CloudUpload) { onNavigate("backupimport") } },
                    { SettingsNavRow(appString(R.string.common_machine_learning), Icons.Filled.Psychology) { onNavigate("machinelearning") } }
                )
            )
            SettingsHeading(appString(R.string.settings_head_privacy))
            SettingsGroup(
                null,
                listOf(
                    { SettingsNavRow(appString(R.string.common_privacy), Icons.Filled.Lock) { onNavigate("security") } },
                    { SettingsNavRow(appString(R.string.common_sharing), Icons.Filled.Share) { onNavigate("exif") } },
                    { SettingsNavRow(appString(R.string.common_hidden_section), Icons.Filled.VisibilityOff) { onNavigate("hiddensection") } }
                )
            )
            SettingsHeading(appString(R.string.settings_head_about))
            SettingsGroup(
                null,
                listOf(
                    { SettingsNavRow(appString(R.string.common_help_support), Icons.AutoMirrored.Filled.Help) { onNavigate("help") } },
                    { SettingsNavRow(appString(R.string.common_about), Icons.Filled.Info) { onNavigate("about") } }
                )
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// Items instead of a content lambda: a divider goes between items, never after the last
@Composable
fun SettingsGroup(title: String?, items: List<@Composable () -> Unit>) {
    if (title != null) {
        Text(
            title,
            color = Color.Gray,
            fontSize = AppText.Body,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = Spacing.Sm)
        )
    }
    Surface(
        color = GlassPill.adaptiveGlassBackground,
        shape = RoundedCornerShape(20.dp),
        border = GlassPill.adaptiveBorder,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        val edgePadding = if (items.size == 1) 4.dp else 8.dp
        Column(Modifier.padding(vertical = edgePadding)) {
            items.forEachIndexed { index, item ->
                item()
                if (index != items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsNavRow(label: String, icon: ImageVector = Icons.Filled.MoreHoriz, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(role = Role.Button) { onClick() }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsIcon(icon)
            Text(label)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null)
    }
}
