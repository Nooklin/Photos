package org.nooklin.photos.ui.hidden

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.nooklin.photos.data.HiddenItem
import org.nooklin.photos.ui.common.SelectionGridPickerScreen
import org.nooklin.photos.ui.common.freshImageRequest

// Same picker as AlbumPhotoPickerScreen, items identified by file name
@Composable
fun HiddenAlbumPhotoPickerScreen(
    albumName: String,
    items: List<HiddenItem>,
    alreadyInAlbum: Set<String>,
    gridColumns: Int,
    onConfirm: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    SelectionGridPickerScreen(
        title = "Add to $albumName",
        items = items,
        key = { it.file.name },
        thumbnailModel = { freshImageRequest(context, it.thumbnailFile, it.thumbnailFile.lastModified()) },
        excludeKeys = alreadyInAlbum,
        gridColumns = gridColumns,
        emptyMessage = "Every hidden photo is already in this album",
        onConfirm = onConfirm,
        onBack = onBack
    )
}
