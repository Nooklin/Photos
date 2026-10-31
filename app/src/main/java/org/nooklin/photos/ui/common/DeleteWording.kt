package org.nooklin.photos.ui.common

import org.nooklin.photos.R
import org.nooklin.photos.appString

// The text of a delete confirmation follows what is deleted: photo, video, or "items" when both are chosen
class DeleteWording(val text: String, val confirmLabel: String)

fun deleteWording(videoFlags: List<Boolean>): DeleteWording {
    val count = videoFlags.size
    val videos = videoFlags.count { it }
    return when {
        count == 1 && videos == 1 -> DeleteWording(
            appString(R.string.common_this_video_will_be_deleted),
            appString(R.string.common_delete_video)
        )
        count == 1 -> DeleteWording(
            appString(R.string.common_this_photo_will_be_deleted),
            appString(R.string.common_delete_photo)
        )
        count > 1 && videos == count -> DeleteWording(
            appString(R.string.common_these_videos_will_be_deleted),
            appString(R.string.common_delete_videos, count)
        )
        videos == 0 -> DeleteWording(
            appString(R.string.common_these_photos_will_be_deleted),
            appString(R.string.common_delete_photos, count)
        )
        else -> DeleteWording(
            appString(R.string.common_these_items_will_be_deleted),
            appString(R.string.common_delete_items, count)
        )
    }
}
