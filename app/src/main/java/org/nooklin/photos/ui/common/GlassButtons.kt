package org.nooklin.photos.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// The Library style, used by every screen: round glass button and glass pill with a text.
// Select, the "..." menu and the close button of a selection all use these.
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 40.dp,
    outerPadding: Dp = 4.dp,
    background: Color = GlassPill.adaptivePopupBackground
) {
    Box(
        Modifier
            .padding(outerPadding)
            .size(size)
            .clip(CircleShape)
            .background(background)
            .border(GlassPill.adaptiveBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
fun PillButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(4.dp)
            .clip(GlassPill.shape)
            .background(GlassPill.adaptivePopupBackground)
            .border(GlassPill.adaptiveBorder, GlassPill.shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text)
    }
}

// Icon in front of a menu item. One size for every menu.
@Composable
fun MenuIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
}
