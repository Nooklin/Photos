package org.nooklin.photos.ui.albums

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.nooklin.photos.data.AlbumRepository
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.local.AlbumEntity
import org.nooklin.photos.data.local.AlbumMembershipEntity
import org.nooklin.photos.ui.common.TaggableCollectionViewModel

class AlbumViewModel(application: Application) :
    TaggableCollectionViewModel<AlbumEntity, AlbumMembershipEntity>(application) {

    private val repository = AlbumRepository(application)
    private var syncJob: Job? = null

    override val collectionFlow: Flow<List<AlbumEntity>> = repository.albums
    override val membershipFlow: Flow<List<AlbumMembershipEntity>> = repository.memberships

    override suspend fun createEntity(name: String): Long = repository.createAlbum(name)
    override suspend fun renameEntity(id: Long, newName: String) = repository.renameAlbum(id, newName)
    override suspend fun addMember(collectionId: Long, mediaId: Long) = repository.addToAlbum(collectionId, mediaId)

    val albums: StateFlow<List<AlbumEntity>> get() = collection

    // Camera, Screenshots, Downloads and WhatsApp: rule key to album id
    val autoAlbums: StateFlow<Map<String, Long>> =
        repository.autoAlbums.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val hiddenAutoAlbumIds: StateFlow<Set<Long>> =
        repository.hiddenAutoAlbumIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun syncAutoAlbums(items: List<MediaItem>) {
        if (items.isEmpty()) return
        syncJob?.cancel()
        syncJob = viewModelScope.launch { repository.syncAutoAlbums(items) }
    }

    fun createAlbum(name: String, onCreated: (Long) -> Unit = {}) = create(name, onCreated)
    fun createFolder(parentId: Long, name: String) {
        viewModelScope.launch { repository.createFolder(parentId, name) }
    }

    fun setCover(albumId: Long, mediaId: Long?) {
        viewModelScope.launch { repository.setCover(albumId, mediaId) }
    }

    fun renameAlbum(albumId: Long, newName: String) = rename(albumId, newName)
    fun addToAlbum(albumId: Long, mediaId: Long) = addToCollection(albumId, mediaId)

    fun removeFromAlbum(albumId: Long, mediaId: Long) {
        viewModelScope.launch { repository.removeFromAlbum(albumId, mediaId) }
    }

    fun moveBetweenAlbums(fromAlbumId: Long, toAlbumId: Long, mediaId: Long) {
        viewModelScope.launch { repository.moveBetweenAlbums(fromAlbumId, toAlbumId, mediaId) }
    }

    // Photos stay, only the album and its memberships go
    fun deleteAlbum(albumId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteAlbum(albumId)
            onDeleted()
        }
    }

    fun mergeAlbums(sourceAlbumId: Long, targetAlbumId: Long, onMerged: () -> Unit = {}) {
        viewModelScope.launch {
            repository.mergeAlbums(sourceAlbumId, targetAlbumId)
            onMerged()
        }
    }

    fun pruneOrphanedMemberships(validMediaIds: Set<Long>) {
        viewModelScope.launch { repository.pruneOrphanedMemberships(validMediaIds) }
    }
}

data class AlbumWithPreview(
    val id: Long,
    val name: String,
    val itemCount: Int,
    val previewItems: List<MediaItem>,
    val parentId: Long? = null,
    val folderCount: Int = 0
)
