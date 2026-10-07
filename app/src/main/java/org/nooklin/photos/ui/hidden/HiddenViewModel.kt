package org.nooklin.photos.ui.hidden

import android.app.Application
import android.app.PendingIntent
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nooklin.photos.data.HiddenAlbumWithPreview
import org.nooklin.photos.data.HiddenItem
import org.nooklin.photos.data.HiddenMediaRepository
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.UnhideResult
import org.nooklin.photos.data.local.LocalMetadataStore
import org.nooklin.photos.ui.MediaFilter
import org.nooklin.photos.ui.SortOrder

data class HiddenUiState(
    val isAuthenticated: Boolean = false,
    // Separate from isAuthenticated, both checks must pass. True when no extra PIN is enabled.
    val pinVerified: Boolean = true,
    val items: List<HiddenItem> = emptyList(),
    val albums: List<HiddenAlbumWithPreview> = emptyList(),
    // (albumId, fileName) pairs to filter items per album client-side, like DiscoveredDetailScreen
    val albumMembership: List<Pair<Long, String>> = emptyList(),
    val isLoading: Boolean = false,
    // Session-only filter and sort state for the Hidden Library, like GalleryUiState
    val filter: MediaFilter = MediaFilter.ALL,
    val hideScreenshots: Boolean = true,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST
) {
    // Computed once per state
    val visibleItems: List<HiddenItem> by lazy(LazyThreadSafetyMode.NONE) {
        items
            .filter { !hideScreenshots || !it.isScreenshot }
            .filter {
                when (filter) {
                    MediaFilter.ALL -> true
                    MediaFilter.FAVORITES -> it.isFavorite
                    MediaFilter.VIDEOS -> it.isVideo
                }
            }
            // Already newest first, OLDEST_FIRST is a reverse
            .let { if (sortOrder == SortOrder.OLDEST_FIRST) it.reversed() else it }
    }
}

class HiddenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HiddenMediaRepository(application, LocalMetadataStore(application))

    private val _uiState = MutableStateFlow(HiddenUiState())
    val uiState: StateFlow<HiddenUiState> = _uiState

    // Hidden copies waiting on the user's answer to the delete prompt for their library originals.
    private var pendingHideCopies: List<String> = emptyList()
    // MediaStore ids of the same items, so the regular library's database rows can be removed once the delete is confirmed.
    private var pendingHideMediaIds: List<Long> = emptyList()

    private val _exportProgress = MutableStateFlow<HiddenExportProgress?>(null)
    val exportProgress: StateFlow<HiddenExportProgress?> = _exportProgress

    fun onAuthenticated() {
        _uiState.update { it.copy(isAuthenticated = true) }
        refresh()
    }

    fun onPinVerified() {
        _uiState.update { it.copy(pinVerified = true) }
    }

    // Re-locks on leaving: biometric and PIN are required again each visit
    fun lock() {
        _uiState.update { it.copy(isAuthenticated = false, pinVerified = false) }
    }

    // If the delete prompt was declined the hidden copy goes again, if accepted the library database rows for the original go too
    fun finishHide(confirmed: Boolean) {
        val copies = pendingHideCopies
        val mediaIds = pendingHideMediaIds
        pendingHideCopies = emptyList()
        pendingHideMediaIds = emptyList()
        if (copies.isEmpty()) return
        viewModelScope.launch {
            if (confirmed) {
                repository.purgeLibraryData(mediaIds)
            } else {
                copies.forEach { repository.discardCopy(it) }
                refresh()
            }
        }
    }

    private var refreshJob: Job? = null

    // Bulk actions call refresh once per item, so a short delay folds a burst into one reload.
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            delay(40)
            _uiState.update { it.copy(isLoading = true) }
            val items = repository.listHidden()
            val albums = repository.listAlbumsWithPreview(items)
            val membership = repository.albumMembershipSnapshot()
            _uiState.update { it.copy(items = items, albums = albums, albumMembership = membership, isLoading = false) }
            backfillVideoThumbnails()
        }
    }

    private var backfillRunning = false

    // Videos hidden before they got thumbnails are filled in once, in the background, then the list reloads.
    private fun backfillVideoThumbnails() {
        if (backfillRunning) return
        backfillRunning = true
        viewModelScope.launch {
            val created = try {
                repository.backfillVideoThumbnails()
            } finally {
                backfillRunning = false
            }
            if (created) refresh()
        }
    }

    fun updateSize(fileName: String, width: Int, height: Int) {
        viewModelScope.launch {
            repository.updateSize(fileName, width, height)
            refresh()
        }
    }

    fun toggleFavorite(item: HiddenItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item.file.name, !item.isFavorite)
            refresh()
        }
    }

    fun setFilter(filter: MediaFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun setHideScreenshots(hide: Boolean) {
        _uiState.update { it.copy(hideScreenshots = hide) }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _uiState.update { it.copy(sortOrder = sortOrder) }
    }

    // Step 1 of hiding: copy to private storage and record metadata. Null if the copy failed, don't delete the original then.
    suspend fun beginHide(item: MediaItem): PendingIntent? {
        val copied = repository.copyToHidden(item)
        if (copied == null) return null
        pendingHideCopies = listOf(copied.name)
        pendingHideMediaIds = listOf(item.id)
        return MediaStore.createDeleteRequest(
            getApplication<Application>().contentResolver,
            listOf(item.uri)
        )
    }

    // One consent dialog for the whole batch (createDeleteRequest takes a list). Failed copies are skipped, null only if all failed.
    suspend fun beginHideMultiple(items: List<MediaItem>): PendingIntent? {
        val copies = items.mapNotNull { item -> repository.copyToHidden(item)?.let { Triple(item.uri, it.name, item.id) } }
        val copiedUris = copies.map { it.first }
        if (copiedUris.isEmpty()) return null
        pendingHideCopies = copies.map { it.second }
        pendingHideMediaIds = copies.map { it.third }
        return MediaStore.createDeleteRequest(
            getApplication<Application>().contentResolver,
            copiedUris
        )
    }

    fun unhide(fileName: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.unhide(fileName)
            refresh()
            if (result is UnhideResult.Success) onComplete()
        }
    }

    fun trash(fileName: String) {
        viewModelScope.launch {
            repository.trash(fileName)
            refresh()
        }
    }

    fun createAlbum(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.createAlbum(name)
            refresh()
            onCreated(id)
        }
    }

    fun renameAlbum(albumId: Long, newName: String) {
        viewModelScope.launch {
            repository.renameAlbum(albumId, newName)
            refresh()
        }
    }

    fun deleteAlbum(albumId: Long) {
        viewModelScope.launch {
            repository.deleteAlbum(albumId)
            refresh()
        }
    }

    fun addToAlbum(albumId: Long, fileName: String) {
        viewModelScope.launch {
            repository.addToAlbum(albumId, fileName)
            refresh()
        }
    }

    fun removeFromAlbum(albumId: Long, fileName: String) {
        viewModelScope.launch {
            repository.removeFromAlbum(albumId, fileName)
            refresh()
        }
    }

    fun exportFlatZip(destination: Uri, onResult: (Boolean) -> Unit) {
        _exportProgress.value = HiddenExportProgress(0, 0)
        viewModelScope.launch {
            val success = repository.exportFlatZip(getApplication(), destination) { done, total ->
                _exportProgress.value = HiddenExportProgress(done, total)
            }
            _exportProgress.value = null
            onResult(success)
        }
    }

    fun exportOrganizedZip(destination: Uri, onResult: (Boolean) -> Unit) {
        _exportProgress.value = HiddenExportProgress(0, 0)
        viewModelScope.launch {
            val success = repository.exportOrganizedZip(getApplication(), destination) { done, total ->
                _exportProgress.value = HiddenExportProgress(done, total)
            }
            _exportProgress.value = null
            onResult(success)
        }
    }

    fun exportSelectedZip(fileNames: Set<String>, destination: Uri, onResult: (Boolean) -> Unit) {
        _exportProgress.value = HiddenExportProgress(0, 0)
        viewModelScope.launch {
            val success = repository.exportSelectedZip(getApplication(), destination, fileNames) { done, total ->
                _exportProgress.value = HiddenExportProgress(done, total)
            }
            _exportProgress.value = null
            onResult(success)
        }
    }
}

// total is 0 until the file list is known, shown as a spinner
data class HiddenExportProgress(val done: Int, val total: Int)
