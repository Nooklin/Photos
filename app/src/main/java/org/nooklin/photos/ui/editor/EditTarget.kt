package org.nooklin.photos.ui.editor

import org.nooklin.photos.data.MediaItem
import java.io.File

sealed class EditTarget {
    data class MediaStoreTarget(val item: MediaItem) : EditTarget()
    data class HiddenFileTarget(val file: File, val thumbnailFile: File, val isVideo: Boolean) : EditTarget()
}
