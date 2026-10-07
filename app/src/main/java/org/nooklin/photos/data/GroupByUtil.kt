package org.nooklin.photos.data

import org.nooklin.photos.data.local.GroupBy
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// dateAdded is in seconds
fun groupLabel(item: MediaItem, groupBy: GroupBy): String = dateGroupLabel(item.dateAdded * 1000L, groupBy)

fun groupItemsByDate(items: List<MediaItem>, groupBy: GroupBy): Map<String, List<MediaItem>> =
    if (groupBy == GroupBy.NONE) emptyMap() else DateLabeler(groupBy).let { labeler -> items.groupBy { labeler.label(it.dateAdded * 1000L) } }

// hiddenAt is already millis
fun hiddenGroupLabel(item: HiddenItem, groupBy: GroupBy): String = dateGroupLabel(item.hiddenAt, groupBy)

fun groupHiddenItemsByDate(items: List<HiddenItem>, groupBy: GroupBy): Map<String, List<HiddenItem>> =
    if (groupBy == GroupBy.NONE) emptyMap() else DateLabeler(groupBy).let { labeler -> items.groupBy { labeler.label(it.hiddenAt) } }

private fun dateGroupLabel(millis: Long, groupBy: GroupBy): String = DateLabeler(groupBy).label(millis)

private class DateLabeler(private val groupBy: GroupBy) {
    private val format: SimpleDateFormat? = when (groupBy) {
        GroupBy.DAY -> SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        GroupBy.MONTH -> SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        GroupBy.YEAR -> SimpleDateFormat("yyyy", Locale.getDefault())
        else -> null
    }

    // ISO weeks: Calendar.YEAR is wrong at the year boundary
    private val calendar: Calendar? = if (groupBy == GroupBy.WEEK) {
        Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
        }
    } else null

    fun label(millis: Long): String = when (groupBy) {
        GroupBy.NONE -> ""
        GroupBy.WEEK -> {
            val cal = calendar!!
            cal.timeInMillis = millis
            val week = cal.get(Calendar.WEEK_OF_YEAR)
            val month = cal.get(Calendar.MONTH)
            val yearForWeek = when {
                week >= 52 && month == Calendar.JANUARY -> cal.get(Calendar.YEAR) - 1
                week == 1 && month == Calendar.DECEMBER -> cal.get(Calendar.YEAR) + 1
                else -> cal.get(Calendar.YEAR)
            }
            "Week $week, $yearForWeek"
        }
        else -> format!!.format(Date(millis))
    }
}
