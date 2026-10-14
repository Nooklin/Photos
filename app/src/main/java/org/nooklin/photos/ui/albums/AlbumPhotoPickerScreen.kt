package org.nooklin.photos.ui.albums

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.ui.common.SelectionGridPickerScreen
import org.nooklin.photos.ui.common.freshImageRequest
import org.nooklin.photos.R
import org.nooklin.photos.appString

// Only shows photos not already in the album, this screen only adds
@Composable
fun AlbumPhotoPickerScreen(
    albumName: String,
    items: List<MediaItem>,
    alreadyInAlbum: Set<Long>,
    gridColumns: Int,
    onConfirm: (List<Long>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    SelectionGridPickerScreen(
        title = appString(R.string.common_add_to, albumName),
        items = items,
        key = { it.id },
        thumbnailModel = { freshImageRequest(context, it.uri, it.dateModified) },
        excludeKeys = alreadyInAlbum,
        gridColumns = gridColumns,
        emptyMessage = appString(R.string.album_photo_picker_every_photo_is_already_in),
        onConfirm = onConfirm,
        onBack = onBack
    )
}
