package org.nooklin.photos

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.crossfade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.nooklin.photos.data.clearShareCache
import org.nooklin.photos.notifications.ScanCompleteNotifier
import org.nooklin.photos.data.local.AppPreferences
import org.nooklin.photos.data.local.PinAttemptGuard
import org.nooklin.photos.data.VideoTrims

class NooklinPhotosApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        AppStrings.app = this
        // Channel must exist before the first scan notification
        ScanCompleteNotifier.createChannelIfNeeded(this)
        CoroutineScope(Dispatchers.IO).launch {
            clearShareCache(this@NooklinPhotosApplication)
            PinAttemptGuard.load(AppPreferences(this@NooklinPhotosApplication))
            VideoTrims.load(AppPreferences(this@NooklinPhotosApplication))
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(MediaStoreThumbnailFetcher.Factory())
                add(VideoFirstFrameFetcher.Factory())
            }
            // More than Coil's default so thumbnails get evicted less
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.35).build() }
            .crossfade(true)
            .build()
    }
}
