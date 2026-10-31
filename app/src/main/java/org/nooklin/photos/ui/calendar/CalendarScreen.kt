package org.nooklin.photos.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.HiddenItem
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.ui.common.freshImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import org.nooklin.photos.ui.common.GridThumbnail
import org.nooklin.photos.ui.common.zoomBackdropSource
import org.nooklin.photos.ui.common.zoomSource
import androidx.compose.ui.semantics.Role
import org.nooklin.photos.ui.common.EmptyState
import org.nooklin.photos.ui.theme.Spacing
import org.nooklin.photos.ui.theme.AppText

private fun dayKey(year: Int, month: Int, day: Int) = year * 10000 + month * 100 + day

// Month per page, swipe left and right. A day with photos shows the first photo of that day, a tap opens the day.
@Composable
fun CalendarScreen(
    items: List<MediaItem>,
    onBack: () -> Unit,
    // All photos of the tapped day, in the order of the Library
    onOpenDay: (List<MediaItem>) -> Unit
) {
    // Same date as the Library headers and the timeline (dateAdded, seconds)
    CalendarContent(
        items = items,
        timeMillis = { it.dateAdded * 1000L },
        zoomKey = { it.id },
        onBack = onBack,
        onOpenDay = onOpenDay
    ) { item, modifier -> GridThumbnail(uri = item.uri, dateModified = item.dateModified, modifier = modifier) }
}

// Same calendar for the Hidden Section: the day is the date the photo was hidden (or its original date after an import)
@Composable
fun HiddenCalendarScreen(
    items: List<HiddenItem>,
    onBack: () -> Unit,
    onOpenDay: (List<HiddenItem>) -> Unit
) {
    val context = LocalContext.current
    CalendarContent(
        items = items,
        timeMillis = { it.hiddenAt },
        zoomKey = { it.file.absolutePath },
        onBack = onBack,
        onOpenDay = onOpenDay
    ) { item, modifier ->
        AsyncImage(
            model = freshImageRequest(context, item.thumbnailFile, item.thumbnailFile.lastModified()),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun <T> CalendarContent(
    items: List<T>,
    timeMillis: (T) -> Long,
    zoomKey: (T) -> Any,
    onBack: () -> Unit,
    onOpenDay: (List<T>) -> Unit,
    thumbnail: @Composable (T, Modifier) -> Unit
) {
    val byDay = remember(items) {
        val calendar = Calendar.getInstance()
        items.groupBy { item ->
            calendar.timeInMillis = timeMillis(item)
            dayKey(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        }
    }
    // Every month from the oldest to the newest photo, also the empty ones in between
    val months = remember(byDay) {
        if (byDay.isEmpty()) {
            emptyList()
        } else {
            val first = byDay.keys.min()
            val last = byDay.keys.max()
            val list = mutableListOf<Pair<Int, Int>>()
            var year = first / 10000
            var month = (first / 100) % 100
            val endYear = last / 10000
            val endMonth = (last / 100) % 100
            while (year < endYear || (year == endYear && month <= endMonth)) {
                list.add(year to month)
                month++
                if (month > 11) {
                    month = 0
                    year++
                }
            }
            list
        }
    }
    val scope = rememberCoroutineScope()

    Scaffold(modifier = Modifier.zoomBackdropSource()) { padding ->
        if (months.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(message = appString(R.string.category_nothing_here_yet), actionLabel = appString(R.string.common_back), onAction = onBack)
            }
            return@Scaffold
        }
        val pagerState = rememberPagerState(initialPage = months.lastIndex) { months.size }
        val (year, month) = months[pagerState.currentPage.coerceIn(0, months.lastIndex)]
        val titleFormat = remember { SimpleDateFormat("LLLL yyyy", Locale.getDefault()) }
        val title = remember(year, month) {
            val calendar = Calendar.getInstance().apply { clear(); set(year, month, 1) }
            titleFormat.format(calendar.time).replaceFirstChar { it.uppercase() }
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(
                        enabled = pagerState.currentPage > 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }
                    ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) }
                    IconButton(
                        enabled = pagerState.currentPage < months.lastIndex,
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
                    ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                }
            )
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val (pageYear, pageMonth) = months[page]
                MonthPage(pageYear, pageMonth, byDay, zoomKey, onOpenDay, thumbnail)
            }
        }
    }
}

@Composable
private fun <T> MonthPage(
    year: Int,
    month: Int,
    byDay: Map<Int, List<T>>,
    zoomKey: (T) -> Any,
    onOpenDay: (List<T>) -> Unit,
    thumbnail: @Composable (T, Modifier) -> Unit
) {
    val firstDay = remember(year, month) { Calendar.getInstance().apply { clear(); set(year, month, 1) } }
    val firstDayOfWeek = Calendar.getInstance().firstDayOfWeek
    val offset = (firstDay.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
    val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
    val rows = (offset + daysInMonth + 6) / 7
    val weekdays = remember { DateFormatSymbols.getInstance().shortWeekdays }
    val today = remember {
        val c = Calendar.getInstance()
        dayKey(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            for (i in 0 until 7) {
                // Calendar.SUNDAY is 1, shortWeekdays is indexed the same way
                val weekday = (firstDayOfWeek - 1 + i) % 7 + 1
                Text(
                    weekdays[weekday],
                    color = Color.Gray,
                    fontSize = AppText.Caption,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val day = row * 7 + column - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp)) {
                        if (day in 1..daysInMonth) {
                            DayCell(day, byDay[dayKey(year, month, day)], dayKey(year, month, day) == today, zoomKey, onOpenDay, thumbnail)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> DayCell(
    day: Int,
    photos: List<T>?,
    isToday: Boolean,
    zoomKey: (T) -> Any,
    onOpenDay: (List<T>) -> Unit,
    thumbnail: @Composable (T, Modifier) -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    val base = Modifier
        .fillMaxSize()
        .clip(shape)
        .let { if (isToday) it.border(2.dp, MaterialTheme.colorScheme.primary, shape) else it }
    if (photos.isNullOrEmpty()) {
        Box(base.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)), contentAlignment = Alignment.TopStart) {
            Text(day.toString(), color = Color.Gray, fontSize = AppText.Caption, modifier = Modifier.padding(Spacing.Xs))
        }
        return
    }
    val cover = photos.first()
    Box(base.zoomSource(zoomKey(cover)).clickable(role = Role.Button) { onOpenDay(photos) }) {
        thumbnail(cover, Modifier.fillMaxSize())
        Text(
            day.toString(),
            color = Color.White,
            fontSize = AppText.Caption,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = Spacing.Xs, vertical = 1.dp)
        )
        if (photos.size > 1) {
            Text(
                photos.size.toString(),
                color = Color.White,
                fontSize = AppText.Caption,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = Spacing.Xs, vertical = 1.dp)
            )
        }
    }
}
