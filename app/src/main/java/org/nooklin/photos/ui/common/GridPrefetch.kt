package org.nooklin.photos.ui.common

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import org.nooklin.photos.data.MediaItem

// Prefetches the cells past the visible area once scrolling slows, same keys and sizes as GridThumbnail so requests are cache hits
@OptIn(FlowPreview::class)
@Composable
fun PrefetchAhead(gridState: LazyGridState, items: List<MediaItem>, columns: Int) {
    val context = LocalContext.current
    LaunchedEffect(gridState, items, columns) {
        val indexById = HashMap<Long, Int>(items.size * 2).also { map -> items.forEachIndexed { i, item -> map[item.id] = i } }
        var lastIndex = -1
        val pending = ArrayList<coil3.request.Disposable>()
        snapshotFlow {
            // The photo cells currently visible (headers have no Long key in the list), as first and last list index.
            val visible = gridState.layoutInfo.visibleItemsInfo.mapNotNull { info -> (info.key as? Long)?.let { indexById[it] } }
            if (visible.isEmpty()) null else Triple(visible.min(), visible.max(), gridState.layoutInfo.visibleItemsInfo.first().size.width)
        }
            .distinctUntilChanged()
            // Waits until the visible cells had their own head start (GridThumbnail delays its sharp decode by 200 ms) so
            // the prefetch does not queue in front of them.
            .debounce(PREFETCH_SETTLE_MS)
            .collect { range ->
                if (range == null) return@collect
                val (first, last, cellPx) = range
                pending.forEach { it.dispose() }
                pending.clear()
                val forward = last >= lastIndex
                lastIndex = last
                val count = columns * PREFETCH_ROWS
                val ahead = if (forward) (last + 1)..minOf(items.size - 1, last + count) else maxOf(0, first - count) until first
                val sharpSize = Size(cellPx.coerceAtLeast(1), cellPx.coerceAtLeast(1))
                for (index in if (forward) ahead else ahead.reversed()) {
                    val item = items[index]
                    val key = "${item.uri}|${item.dateModified}"
                    if (key in sharpShown) continue
                    pending += context.imageLoader.enqueue(
                        ImageRequest.Builder(context)
                            .data(item.uri)
                            .size(Size(QUICK_SIZE_PX, QUICK_SIZE_PX))
                            .memoryCacheKey("$key|quick")
                            .diskCacheKey("$key|quick")
                            .build()
                    )
                    pending += context.imageLoader.enqueue(
                        ImageRequest.Builder(context)
                            .data(item.uri)
                            .size(sharpSize)
                            .memoryCacheKey(key)
                            .diskCacheKey(key)
                            .listener(object : ImageRequest.Listener {
                                override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                                    sharpShown.add(key)
                                }
                            })
                            .build()
                    )
                }
            }
    }
}

private const val PREFETCH_SETTLE_MS = 350L
private const val PREFETCH_ROWS = 2
