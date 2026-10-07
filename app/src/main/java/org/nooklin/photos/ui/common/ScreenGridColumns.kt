package org.nooklin.photos.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.local.AppPreferences

// Per-screen column count, remembered. Falls back to the Library's value.
class ScreenGridColumns(val columns: Int, val setColumns: (Int) -> Unit)

@Composable
fun rememberScreenGridColumns(key: String, default: Int): ScreenGridColumns {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }
    val flow = remember(key) { preferences.screenGridColumns(key) }
    val stored by flow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    return ScreenGridColumns(stored ?: default) { count ->
        scope.launch { preferences.setScreenGridColumns(key, count) }
    }
}

/** Width divided by height of a photo or video, 1 when the size is unknown. Used by 1 column grids. */
fun MediaItem.cellAspectRatio(): Float =
    if (width != null && height != null && height > 0) width.toFloat() / height.toFloat() else 1f
