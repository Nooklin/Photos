package org.nooklin.photos.ui.settings

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.unit.sp
import org.nooklin.photos.ui.common.GlassPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
                null,
                listOf(
                    { SettingsNavRow("Appearance", Icons.Filled.Palette) { onNavigate("appearance") } },
                    { SettingsNavRow("Gallery", Icons.Filled.PhotoLibrary) { onNavigate("gallery") } },
                    { SettingsNavRow("Privacy", Icons.Filled.Lock) { onNavigate("security") } },
                    { SettingsNavRow("Sharing", Icons.Filled.Share) { onNavigate("exif") } },
                    { SettingsNavRow("Machine Learning", Icons.Filled.Psychology) { onNavigate("machinelearning") } },
                    { SettingsNavRow("Notifications", Icons.Filled.Notifications) { onNavigate("notifications") } },
                    { SettingsNavRow("Hidden Section", Icons.Filled.VisibilityOff) { onNavigate("hiddensection") } },
                    { SettingsNavRow("Help & Support", Icons.AutoMirrored.Filled.Help) { onNavigate("help") } },
                    { SettingsNavRow("About", Icons.Filled.Info) { onNavigate("about") } }
                )
            )
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
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 6.dp)
        )
    }
    Surface(
        // adaptiveGlassBackground like Home's GlassBlock, adaptivePopupBackground made the rows unreadable
        color = GlassPill.adaptiveGlassBackground,
        shape = RoundedCornerShape(20.dp),
        border = GlassPill.adaptiveBorder,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        val edgePadding = if (items.size == 1) 8.dp else 14.dp
        Column(Modifier.padding(vertical = edgePadding)) {
            items.forEachIndexed { index, item ->
                item()
                if (index != items.lastIndex) {
                    // start = 56dp aligns past the leading icon
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
            .heightIn(min = 52.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
            Text(label)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null)
    }
}
