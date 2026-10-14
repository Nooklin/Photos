package org.nooklin.photos.data

import androidx.annotation.StringRes
import org.nooklin.photos.R
import org.nooklin.photos.appString

// Only categories with a real signal, no Live/Selfie/Portrait/Burst
enum class MediaCategory(@StringRes val labelRes: Int) {
    VIDEOS(R.string.common_videos),
    PANORAMAS(R.string.common_panoramas),
    SCREENSHOTS(R.string.common_screenshots),
    SCREEN_RECORDINGS(R.string.common_screen_recordings),
    SLOW_MOTION(R.string.common_slow_motion),
    RAW(R.string.media_category_raw),
    FAVORITES(R.string.media_category_favourites),
    RECENTLY_SAVED(R.string.common_recently_saved),
    DUPLICATES(R.string.media_category_duplicates);
    val label: String get() = appString(labelRes)
}

private const val RECENTLY_SAVED_WINDOW_MILLIS = 30L * 24 * 60 * 60 * 1000

fun filterByCategory(items: List<MediaItem>, category: MediaCategory): List<MediaItem> =
    when (category) {
        MediaCategory.VIDEOS -> items.filter { it.isVideo }
        MediaCategory.SCREENSHOTS -> items.filter { it.isScreenshot }
        MediaCategory.SCREEN_RECORDINGS -> items.filter { it.isScreenRecording }
        MediaCategory.SLOW_MOTION -> items.filter { it.isSlowMotion }
        MediaCategory.RAW -> items.filter { it.isRaw }
        MediaCategory.PANORAMAS -> items.filter { it.isPanorama }
        MediaCategory.FAVORITES -> items.filter { it.isFavorite }
        MediaCategory.RECENTLY_SAVED -> {
            val cutoff = System.currentTimeMillis() - RECENTLY_SAVED_WINDOW_MILLIS
            items.filter { it.dateAdded * 1000 >= cutoff }.sortedByDescending { it.dateAdded }
        }
        // Handled in CategoryScreen, never reached
        MediaCategory.DUPLICATES -> emptyList()
    }
