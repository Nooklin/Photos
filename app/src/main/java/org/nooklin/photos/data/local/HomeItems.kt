package org.nooklin.photos.data.local

import org.nooklin.photos.data.MediaCategory

object HomeItems {
    val MEDIA_TYPES = listOf("VIDEOS", "PANORAMAS", "SCREENSHOTS", "SCREEN_RECORDINGS", "SLOW_MOTION", "RAW")
    val UTILITIES = listOf("FAVORITES", "HIDDEN", "RECENTLY_SAVED", "RECENTLY_DELETED", "DUPLICATES")

    fun label(key: String): String = when (key) {
        "HIDDEN" -> "Hidden Section"
        "RECENTLY_DELETED" -> "Recently Deleted"
        else -> MediaCategory.entries.find { it.name == key }?.label ?: key
    }

    // Unknown saved keys go at the end
    fun ordered(defaults: List<String>, saved: List<String>): List<String> {
        val known = saved.filter { it in defaults }.distinct()
        return known + defaults.filter { it !in known }
    }
}
