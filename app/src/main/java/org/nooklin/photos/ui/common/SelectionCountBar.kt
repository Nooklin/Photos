package org.nooklin.photos.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.nooklin.photos.R
import org.nooklin.photos.appString

// The "N selected" pill at the bottom while selecting
@Composable
fun SelectionCountBar(count: Int, modifier: Modifier = Modifier) {
    Surface(
        color = GlassPill.adaptivePopupBackground,
        shape = GlassPill.shape,
        border = GlassPill.adaptiveBorder,
        modifier = modifier
    ) {
        Box(Modifier.height(56.dp).widthIn(min = 176.dp).padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
            Text(appString(R.string.share_sheet_selected, count), fontWeight = FontWeight.SemiBold)
        }
    }
}
