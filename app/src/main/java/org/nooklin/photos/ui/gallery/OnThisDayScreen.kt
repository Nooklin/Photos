package org.nooklin.photos.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.ui.common.EmptyState
import org.nooklin.photos.ui.common.ScrollToPendingZoomKey
import org.nooklin.photos.ui.common.scrollToItemCentered
import org.nooklin.photos.ui.common.zoomBackdropSource
import org.nooklin.photos.ui.common.zoomSource
import java.util.Calendar

// items taken on today's month and day in an earlier year, newest year first
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OnThisDayScreen(
    items: List<T>,
    timeMillis: (T) -> Long,
    zoomKey: (T) -> Any,
    gridColumns: Int,
    onBack: () -> Unit,
    onItemClick: (all: List<T>, index: Int) -> Unit,
    thumbnail: @Composable (T, Modifier) -> Unit
) {
    val today = remember { Calendar.getInstance() }
    val groups = remember(items) {
        val month = today.get(Calendar.MONTH)
        val day = today.get(Calendar.DAY_OF_MONTH)
        val year = today.get(Calendar.YEAR)
        val cal = Calendar.getInstance()
        items.filter { item ->
            cal.timeInMillis = timeMillis(item)
            cal.get(Calendar.MONTH) == month && cal.get(Calendar.DAY_OF_MONTH) == day && cal.get(Calendar.YEAR) < year
        }.groupBy { item ->
            cal.timeInMillis = timeMillis(item)
            cal.get(Calendar.YEAR)
        }.toSortedMap(compareByDescending { it })
    }
    val flat = remember(groups) { groups.values.flatten() }
    val gridState = rememberLazyGridState()
    ScrollToPendingZoomKey(
        lazyIndexOf = { key ->
            var index = 0
            var found: Int? = null
            for (group in groups.values) {
                index++ // header
                val at = group.indexOfFirst { zoomKey(it) == key }
                if (at >= 0) { found = index + at; break }
                index += group.size
            }
            found
        },
        scrollToIndex = { i -> gridState.scrollToItemCentered(i) }
    )

    Scaffold(
        modifier = Modifier.zoomBackdropSource(),
        topBar = {
            TopAppBar(
                title = { Text(appString(R.string.home_on_this_day)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        if (flat.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(message = appString(R.string.on_this_day_empty), actionLabel = appString(R.string.common_back), onAction = onBack)
            }
            return@Scaffold
        }
        val thisYear = today.get(Calendar.YEAR)
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(padding)
        ) {
            var offset = 0
            for ((year, group) in groups) {
                val start = offset
                item(span = { GridItemSpan(maxLineSpan) }, key = "year$year") {
                    Text(
                        text = "$year  " + pluralStringResource(R.plurals.on_this_day_years_ago, thisYear - year, thisYear - year),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp)
                    )
                }
                items(group.size, key = { "item${zoomKey(group[it])}" }) { index ->
                    val item = group[index]
                    thumbnail(
                        item,
                        Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .zoomSource(zoomKey(item))
                            .clickable { onItemClick(flat, start + index) }
                    )
                }
                offset += group.size
            }
        }
    }
}
