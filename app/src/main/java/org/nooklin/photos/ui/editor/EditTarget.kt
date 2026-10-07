package org.nooklin.photos.ui.editor

import org.nooklin.photos.data.MediaItem
import java.io.File

// Two save paths: MediaStore needs ContentResolver and a write consent, hidden files are ours
sealed class EditTarget {
    data class MediaStoreTarget(val item: MediaItem) : EditTarget()
    // thumbnailFile is the grid's thumbnail, regenerate it on save or the grid shows the old one
    data class HiddenFileTarget(val file: File, val thumbnailFile: File, val isVideo: Boolean) : EditTarget()
}
