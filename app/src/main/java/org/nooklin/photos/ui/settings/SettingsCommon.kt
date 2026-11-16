package org.nooklin.photos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nooklin.photos.appString
import org.nooklin.photos.R
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Column
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.theme.AppText
import androidx.compose.material3.MaterialTheme

@Composable
fun SettingsHint(text: String) {
    Text(
        text,
        fontSize = AppText.Caption,
        color = Color.Gray,
        modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsHint(text: androidx.compose.ui.text.AnnotatedString) {
    Text(
        text,
        fontSize = AppText.Caption,
        color = Color.Gray,
        modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsHeading(title: String) {
    Text(
        title,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 28.dp, bottom = 8.dp)
    )
}

@Composable
fun <T> SettingsChoiceRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    fontFor: (T) -> FontFamily? = { null },
    onSelect: (T) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected }?.second.orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(role = Role.Button) { open = true }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(current, color = Color.Gray)
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            shape = GlassPill.shape,
            containerColor = GlassPill.adaptivePopupBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(label) },
            text = {
                Column {
                    options.forEach { (value, text) ->
                        SettingsRadioRow(text, value == selected, fontFamily = fontFor(value)) { onSelect(value); open = false }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text(appString(R.string.common_cancel)) } }
        )
    }
}

// RadioButton keeps a real onClick: with null the dot didn't visually update on a device.
@Composable
fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    fontFamily: FontFamily? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(role = Role.RadioButton) { onClick() }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The row is the touch target, the 48 dp the button asks for would make the row taller than the main menu rows
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            RadioButton(selected = selected, onClick = onClick)
        }
        Text(label, fontFamily = fontFamily, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    icon: ImageVector,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(role = Role.Switch, enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsIcon(icon, enabled)
            Text(label, color = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f))
        }
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

/** Whether the icons in the settings show on a coloured tile (Appearance, Colorful display). */
val LocalColorful = staticCompositionLocalOf { true }

object Tile {
    val Blue = Color(0xFF0A84FF)
    val Green = Color(0xFF30D158)
    val Orange = Color(0xFFFF9F0A)
    val Red = Color(0xFFFF453A)
    val Purple = Color(0xFFBF5AF2)
    val Gray = Color(0xFF8E8E93)
}

// one colour per icon, so a setting looks the same wherever it appears
private fun tileFor(icon: ImageVector): Color = when (icon) {
    Icons.Filled.Palette, Icons.Filled.Psychology, Icons.Filled.Lightbulb, Icons.Filled.CameraAlt -> Tile.Orange
    Icons.Filled.Notifications, Icons.AutoMirrored.Filled.Help, Icons.Filled.LocationOff,
    Icons.Filled.LocationOn, Icons.Filled.Schedule -> Tile.Green
    Icons.Filled.Lock, Icons.Filled.PrivacyTip, Icons.Filled.Screenshot, Icons.Filled.BugReport -> Tile.Red
    Icons.Filled.VisibilityOff, Icons.Filled.Password -> Tile.Purple
    Icons.Filled.Info, Icons.Filled.History, Icons.Filled.Code, Icons.Filled.Description -> Tile.Gray
    else -> Tile.Blue
}

/** The icon of a settings row: white on a coloured rounded tile, or the plain icon when the colourful display is off. */
@Composable
fun SettingsIcon(icon: ImageVector, enabled: Boolean = true) {
    val alpha = if (enabled) 1f else 0.38f
    if (LocalColorful.current) {
        Box(
            modifier = Modifier
                .padding(end = 12.dp)
                .size(30.dp)
                .alpha(alpha)
                .clip(RoundedCornerShape(8.dp))
                .background(tileFor(icon)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    } else {
        Icon(
            icon,
            contentDescription = null,
            tint = LocalContentColor.current.copy(alpha = alpha),
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}
