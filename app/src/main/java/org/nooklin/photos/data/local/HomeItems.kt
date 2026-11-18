package org.nooklin.photos.data.local

import org.nooklin.photos.data.MediaCategory
import org.nooklin.photos.R
import org.nooklin.photos.appString

object HomeItems {
    val MEDIA_TYPES = listOf("VIDEOS", "PANORAMAS", "SCREENSHOTS", "SCREEN_RECORDINGS", "SLOW_MOTION", "RAW")
    val UTILITIES = listOf("FAVORITES", "HIDDEN", "RECENTLY_SAVED", "RECENTLY_DELETED", "DUPLICATES", "CALENDAR", "ON_THIS_DAY")

    fun label(key: String): String = when (key) {
        "HIDDEN" -> appString(R.string.common_hidden_section)
        "RECENTLY_DELETED" -> appString(R.string.common_recently_deleted)
        "CALENDAR" -> appString(R.string.home_calendar)
        "ON_THIS_DAY" -> appString(R.string.home_on_this_day)
        else -> MediaCategory.entries.find { it.name == key }?.label ?: key
    }

    fun ordered(defaults: List<String>, saved: List<String>): List<String> {
        val known = saved.filter { it in defaults }.distinct()
        return known + defaults.filter { it !in known }
    }
}
