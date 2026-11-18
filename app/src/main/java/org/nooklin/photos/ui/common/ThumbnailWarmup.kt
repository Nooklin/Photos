package org.nooklin.photos.ui.common

import android.content.Context
import android.os.SystemClock
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.ThumbnailStore

// Loads the newest thumbnails in the background (after an import, or on opening the app), with the same requests the grid makes
object ThumbnailWarmup {
    private const val MAX_ITEMS = 800
    private const val QUIET_MS = 800L

    @Volatile
    private var lastScrollMs = 0L
    private var job: Job? = null
    private var hiddenJob: Job? = null
    private var storeJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun noteScroll() {
        lastScrollMs = SystemClock.elapsedRealtime()
    }

    fun start(context: Context, items: List<MediaItem>, cellPx: Int) {
        job?.cancel()
        val app = context.applicationContext
        job = scope.launch {
            val loader = app.imageLoader
            val sharpSize = Size(cellPx.coerceAtLeast(1), cellPx.coerceAtLeast(1))
            val todo = items.asSequence()
                .filter { "${it.uri}|${it.dateModified}" !in sharpShown }
                .take(MAX_ITEMS)
                .toList()
            val slots = Semaphore(2)
            coroutineScope {
                for (item in todo) {
                    // waits while the user scrolls, the visible cells go first
                    while (SystemClock.elapsedRealtime() - lastScrollMs < QUIET_MS) delay(200)
                    slots.acquire()
                    launch {
                        try {
                            val key = "${item.uri}|${item.dateModified}"
                            loader.execute(
                                ImageRequest.Builder(app)
                                    .data(item.uri)
                                    .size(Size(QUICK_SIZE_PX, QUICK_SIZE_PX))
                                    .memoryCacheKey("$key|quick")
                                    .diskCacheKey("$key|quick")
                                    .build()
                            )
                            loader.execute(
                                ImageRequest.Builder(app)
                                    .data(item.uri)
                                    .size(sharpSize)
                                    .memoryCacheKey(key)
                                    .diskCacheKey(key)
                                    .listener(object : ImageRequest.Listener {
                                        override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                                            markSharpShown(key)
                                        }
                                    })
                                    .build()
                            )
                        } finally {
                            slots.release()
                        }
                    }
                }
            }
        }
    }

    // The Hidden thumbnails are private files, only warmed while the Hidden Section is unlocked
    fun startHidden(context: Context, thumbnails: List<java.io.File>, cellPx: Int) {
        hiddenJob?.cancel()
        val app = context.applicationContext
        hiddenJob = scope.launch {
            val loader = app.imageLoader
            val size = Size(cellPx.coerceAtLeast(1), cellPx.coerceAtLeast(1))
            val slots = Semaphore(2)
            coroutineScope {
                for (file in thumbnails.take(MAX_ITEMS)) {
                    while (SystemClock.elapsedRealtime() - lastScrollMs < QUIET_MS) delay(200)
                    slots.acquire()
                    launch {
                        try {
                            loader.execute(freshImageRequest(app, file, file.lastModified()).newBuilder().size(size).build())
                        } finally {
                            slots.release()
                        }
                    }
                }
            }
        }
    }

    // The whole library into the thumbnail store, newest first, without filling the memory cache
    suspend fun fillStore(context: Context, items: List<MediaItem>) {
        val app = context.applicationContext
        val loader = app.imageLoader
        val slots = Semaphore(2)
        coroutineScope {
            for (item in items) {
                val key = ThumbnailStore.keyOf(item)
                if (ThumbnailStore.has(app, key)) continue
                while (SystemClock.elapsedRealtime() - lastScrollMs < QUIET_MS) delay(200)
                slots.acquire()
                launch {
                    try {
                        loader.execute(
                            ImageRequest.Builder(app)
                                .data(item.uri)
                                .size(Size(ThumbnailStore.STORE_PX, ThumbnailStore.STORE_PX))
                                .diskCacheKey(key)
                                .memoryCachePolicy(coil3.request.CachePolicy.DISABLED)
                                .build()
                        )
                    } finally {
                        slots.release()
                    }
                }
            }
        }
    }

    fun startFillStore(context: Context, items: List<MediaItem>) {
        storeJob?.cancel()
        val app = context.applicationContext
        storeJob = scope.launch {
            fillStore(app, items)
            ThumbnailStore.cleanUp(app, items)
        }
    }
}
