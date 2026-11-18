package org.nooklin.photos.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.nooklin.photos.R
import org.nooklin.photos.appString
import java.util.Calendar

class SearchFilterState {
    var photosOnly by mutableStateOf(false)
    var videosOnly by mutableStateOf(false)
    var favoritesOnly by mutableStateOf(false)
    var year by mutableStateOf<Int?>(null)

    val active: Boolean get() = photosOnly || videosOnly || favoritesOnly || year != null

    fun matches(isVideo: Boolean, isFavorite: Boolean, timeMillis: Long): Boolean =
        (!photosOnly || !isVideo) &&
            (!videosOnly || isVideo) &&
            (!favoritesOnly || isFavorite) &&
            (year == null || yearOf(timeMillis) == year)
}

fun yearOf(timeMillis: Long): Int =
    Calendar.getInstance().apply { timeInMillis = timeMillis }.get(Calendar.YEAR)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterRow(state: SearchFilterState, years: List<Int>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            GlassChip(
                selected = state.photosOnly,
                onClick = { state.photosOnly = !state.photosOnly; if (state.photosOnly) state.videosOnly = false },
                label = { Text(appString(R.string.search_filter_photos)) }
            )
        }
        item {
            GlassChip(
                selected = state.videosOnly,
                onClick = { state.videosOnly = !state.videosOnly; if (state.videosOnly) state.photosOnly = false },
                label = { Text(appString(R.string.common_videos)) }
            )
        }
        item {
            GlassChip(
                selected = state.favoritesOnly,
                onClick = { state.favoritesOnly = !state.favoritesOnly },
                label = { Text(appString(R.string.common_favorites)) }
            )
        }
        items(years) { year ->
            GlassChip(
                selected = state.year == year,
                onClick = { state.year = if (state.year == year) null else year },
                label = { Text(year.toString()) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        shape = GlassPill.shape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = GlassPill.adaptivePopupBackground,
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        ),
        border = GlassPill.adaptiveBorder
    )
}
