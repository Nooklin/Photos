package org.nooklin.photos.ui

import org.nooklin.photos.data.local.AppPreferences
import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.MediaRepository
import org.nooklin.photos.data.MediaStoreRepository
import org.nooklin.photos.data.OriginalsStore
import org.nooklin.photos.data.local.LocalMetadataStore

enum class MediaFilter { ALL, FAVORITES, VIDEOS }
enum class SortOrder { NEWEST_FIRST, OLDEST_FIRST }

data class GalleryUiState(
    val allItems: List<MediaItem> = emptyList(),
    val filter: MediaFilter = MediaFilter.ALL,
    val hideScreenshots: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val isLoading: Boolean = true,
    val visibleItems: List<MediaItem> = emptyList()
)

private fun GalleryUiState.computeVisible(): List<MediaItem> =
    allItems
        .filter { !hideScreenshots || !it.isScreenshot }
        .filter {
            when (filter) {
                MediaFilter.ALL -> true
                MediaFilter.FAVORITES -> it.isFavorite
                MediaFilter.VIDEOS -> it.isVideo
            }
        }
        .let { if (sortOrder == SortOrder.OLDEST_FIRST) it.reversed() else it }

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val localMetadataStore = LocalMetadataStore(application)
    private val repository = MediaRepository(MediaStoreRepository(application), localMetadataStore, OriginalsStore(application))

    private val _uiState = MutableStateFlow(GalleryUiState())

    // visible list is built off the main thread, and only when one of its inputs changed
    val uiState: StateFlow<GalleryUiState> = run {
        var last: GalleryUiState? = null
        var lastVisible: List<MediaItem> = emptyList()
        _uiState.map { s ->
            val prev = last
            if (prev == null || prev.allItems !== s.allItems || prev.filter != s.filter ||
                prev.hideScreenshots != s.hideScreenshots || prev.sortOrder != s.sortOrder
            ) lastVisible = s.computeVisible()
            last = s
            s.copy(visibleItems = lastVisible)
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, GalleryUiState())
    }

    val recycleBinEntries: StateFlow<List<LocalMetadataStore.RecycleBinEntry>> =
        repository.observeRecycleBin()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var pendingReload: Job? = null
    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
            pendingReload?.cancel()
            pendingReload = viewModelScope.launch {
                delay(500)
                load()
            }
        }
    }

    init {
        viewModelScope.launch {
            AppPreferences(application).showScreenshotsInLibrary.collect { show ->
                _uiState.update { it.copy(hideScreenshots = !show) }
            }
        }
        val resolver = getApplication<Application>().contentResolver
        resolver.registerContentObserver(MediaStore.Files.getContentUri("external"), true, mediaObserver)
        resolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mediaObserver)
        resolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, mediaObserver)
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaObserver)
    }

    private var loadJob: Job? = null

    // Edit in place instead of re-querying MediaStore
    private fun updateItemsLocally(transform: (List<MediaItem>) -> List<MediaItem>) {
        _uiState.update { it.copy(allItems = transform(it.allItems)) }
        // A running load would overwrite this edit
        if (loadJob?.isActive == true) load()
    }

    fun removeItem(id: Long) {
        updateItemsLocally { items -> items.filterNot { it.id == id } }
    }

    fun load() {
        // an older load must not overwrite a newer one
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (_uiState.value.allItems.isEmpty()) _uiState.update { it.copy(isLoading = true) }
            val items = repository.loadAll()
            // keep the old instances for unchanged items so grid cells skip recomposition
            val old = _uiState.value.allItems
            val merged = withContext(Dispatchers.Default) {
                val byId = old.associateBy { it.id }
                items.map { new -> byId[new.id]?.takeIf { it == new } ?: new }
            }
            _uiState.update { state ->
                if (merged == state.allItems) state.copy(isLoading = false) else state.copy(allItems = merged, isLoading = false)
            }
        }
    }

    fun setFilter(filter: MediaFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    suspend fun findMediaItemById(mediaId: Long): MediaItem? = repository.findById(mediaId)

    fun setSortOrder(sortOrder: SortOrder) {
        _uiState.update { it.copy(sortOrder = sortOrder) }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
            val favorite = !item.isFavorite
            updateItemsLocally { items -> items.map { if (it.id == item.id) it.copy(isFavorite = favorite) else it } }
        }
    }

    fun moveToRecycleBin(item: MediaItem) {
        viewModelScope.launch {
            repository.moveToRecycleBin(item)
            updateItemsLocally { items -> items.filterNot { it.id == item.id } }
        }
    }

    fun restoreFromRecycleBin(mediaId: Long) {
        viewModelScope.launch {
            repository.restoreFromRecycleBin(mediaId)
            load()
        }
    }

    // After expired items are deleted (or denied) only drop our tracking, denied ones become visible again
    fun purgeExpiredFromRecycleBin(mediaIds: List<Long>) {
        viewModelScope.launch {
            mediaIds.forEach { repository.restoreFromRecycleBin(it) }
            load()
        }
    }
}
