package org.nooklin.photos.ui.albums

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.nooklin.photos.data.AlbumRepository
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.local.AlbumEntity
import org.nooklin.photos.data.local.AlbumMembershipEntity
import org.nooklin.photos.ui.common.TaggableCollectionViewModel

class AlbumViewModel(application: Application) :
    TaggableCollectionViewModel<AlbumEntity, AlbumMembershipEntity>(application) {

    private val repository = AlbumRepository(application)
    private var hasSeeded = false

    override val collectionFlow: Flow<List<AlbumEntity>> = repository.albums
    override val membershipFlow: Flow<List<AlbumMembershipEntity>> = repository.memberships

    override suspend fun createEntity(name: String): Long = repository.createAlbum(name)
    override suspend fun renameEntity(id: Long, newName: String) = repository.renameAlbum(id, newName)
    override suspend fun addMember(collectionId: Long, mediaId: Long) = repository.addToAlbum(collectionId, mediaId)

    val albums: StateFlow<List<AlbumEntity>> get() = collection

    // Session guard on top of the DB check
    fun seedIfNeeded(items: List<MediaItem>) {
        if (hasSeeded || items.isEmpty()) return
        hasSeeded = true
        viewModelScope.launch { repository.seedFromFoldersIfNeeded(items) }
    }

    fun createAlbum(name: String, onCreated: (Long) -> Unit = {}) = create(name, onCreated)
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
    val previewItems: List<MediaItem>
)
