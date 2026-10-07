package org.nooklin.photos.data

// Only categories with a real signal, no Live/Selfie/Portrait/Burst
enum class MediaCategory(val label: String) {
    VIDEOS("Videos"),
    PANORAMAS("Panoramas"),
    SCREENSHOTS("Screenshots"),
    SCREEN_RECORDINGS("Screen Recordings"),
    SLOW_MOTION("Slow Motion"),
    RAW("RAW"),
    FAVORITES("Favourites"),
    RECENTLY_SAVED("Recently Saved"),
    DUPLICATES("Duplicates")
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
