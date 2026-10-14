package org.nooklin.photos.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.ui.common.GlassPill
import org.nooklin.photos.ui.common.freshImageRequest

// Years and Months give an overview with one photo per period, All is the normal Library grid
enum class TimelineMode { YEARS, MONTHS, ALL }

private class TimelinePeriod(
    val year: Int,
    val label: String,
    val cover: MediaItem,
    val count: Int,
    // First photo of the period in the current sort order, where the Library grid jumps to
    val firstItemId: Long
)

// Items keep their order, so periods follow the sort order too. Same date as the grid headers (dateAdded, seconds).
private fun buildPeriods(items: List<MediaItem>, months: Boolean): List<TimelinePeriod> {
    val calendar = Calendar.getInstance()
    val format = SimpleDateFormat(if (months) "MMM yyyy" else "yyyy", Locale.getDefault())
    val groups = LinkedHashMap<Int, MutableList<MediaItem>>()
    for (item in items) {
        calendar.timeInMillis = item.dateAdded * 1000L
        val key = calendar.get(Calendar.YEAR) * 100 + if (months) calendar.get(Calendar.MONTH) else 0
        groups.getOrPut(key) { mutableListOf() }.add(item)
    }
    return groups.map { (key, group) ->
        TimelinePeriod(
            year = key / 100,
            label = format.format(Date(group.first().dateAdded * 1000L)),
            // A still photo as the cover when the period has one
            cover = group.firstOrNull { !it.isVideo } ?: group.first(),
            count = group.size,
            firstItemId = group.first().id
        )
    }
}

@Composable
fun TimelineOverview(
    items: List<MediaItem>,
    mode: TimelineMode,
    // Months view starts at this year when it comes from a tap in the Years view
    focusYear: Int?,
    onOpenYear: (Int) -> Unit,
    onOpenMonth: (firstItemId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val months = mode == TimelineMode.MONTHS
    val periods = remember(items, months) { buildPeriods(items, months) }
    // New state per view, so Months does not start at the scroll position of Years
    val gridState = remember(months) { LazyGridState() }
    LaunchedEffect(months, focusYear) {
        if (months && focusYear != null) {
            val index = periods.indexOfFirst { it.year == focusYear }
            if (index >= 0) gridState.scrollToItem(index)
        }
    }
    val context = LocalContext.current
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(if (months) 3 else 2),
        // Room at the bottom for the bar and the search button
        contentPadding = PaddingValues(start = 2.dp, top = 2.dp, end = 2.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(periods, key = { it.label }) { period ->
            Box(
                Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { if (months) onOpenMonth(period.firstItemId) else onOpenYear(period.year) }
            ) {
                AsyncImage(
                    model = freshImageRequest(context, period.cover.uri, period.cover.dateModified),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            period.label,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (months) 15.sp else 22.sp
                        )
                        Text(appString(R.string.common_items, period.count), color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// Floating glass pill with Years, Months and All, like the iOS Photos bar
@Composable
fun TimelineBar(selected: TimelineMode, onSelect: (TimelineMode) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        // Near opaque like the menus: the glass background was hard to see over photos
        color = GlassPill.adaptivePopupBackground,
        shape = GlassPill.shape,
        border = GlassPill.adaptiveBorder,
        modifier = modifier
    ) {
        Row(Modifier.padding(4.dp)) {
            listOf(
                TimelineMode.YEARS to R.string.timeline_years,
                TimelineMode.MONTHS to R.string.timeline_months,
                TimelineMode.ALL to R.string.timeline_all
            ).forEach { (mode, label) ->
                val isSelected = mode == selected
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else Color.Transparent)
                        .clickable { onSelect(mode) }
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        appString(label),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
