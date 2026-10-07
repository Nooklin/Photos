package org.nooklin.photos.ui.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Shared by AlbumViewModel and PetViewModel
abstract class TaggableCollectionViewModel<Entity, Membership>(
    application: Application
) : AndroidViewModel(application) {
    protected abstract val collectionFlow: Flow<List<Entity>>
    protected abstract val membershipFlow: Flow<List<Membership>>

    protected abstract suspend fun createEntity(name: String): Long
    protected abstract suspend fun renameEntity(id: Long, newName: String)
    protected abstract suspend fun addMember(collectionId: Long, mediaId: Long)

    val collection: StateFlow<List<Entity>> by lazy {
        collectionFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val memberships: StateFlow<List<Membership>> by lazy {
        membershipFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun create(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch { onCreated(createEntity(name)) }
    }

    fun rename(id: Long, newName: String) {
        viewModelScope.launch { renameEntity(id, newName) }
    }

    fun addToCollection(collectionId: Long, mediaId: Long) {
        viewModelScope.launch { addMember(collectionId, mediaId) }
    }
}
