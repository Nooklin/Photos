package org.nooklin.photos.ui.pets

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.PetRepository
import org.nooklin.photos.data.local.PetEntity
import org.nooklin.photos.data.local.PetMembershipEntity
import org.nooklin.photos.ui.common.TaggableCollectionViewModel

class PetViewModel(application: Application) :
    TaggableCollectionViewModel<PetEntity, PetMembershipEntity>(application) {

    private val repository = PetRepository(application)

    override val collectionFlow: Flow<List<PetEntity>> = repository.pets
    override val membershipFlow: Flow<List<PetMembershipEntity>> = repository.memberships

    override suspend fun createEntity(name: String): Long = repository.createPet(name)
    override suspend fun renameEntity(id: Long, newName: String) = repository.renamePet(id, newName)
    override suspend fun addMember(collectionId: Long, mediaId: Long) = repository.addToPet(collectionId, mediaId)

    val pets: StateFlow<List<PetEntity>> get() = collection

    fun createPet(name: String, onCreated: (Long) -> Unit = {}) = create(name, onCreated)
    fun renamePet(petId: Long, newName: String) = rename(petId, newName)
    fun addToPet(petId: Long, mediaId: Long) = addToCollection(petId, mediaId)

    fun deletePet(petId: Long) {
        viewModelScope.launch { repository.deletePet(petId) }
    }

    fun removeFromPet(petId: Long, mediaId: Long) {
        viewModelScope.launch { repository.removeFromPet(petId, mediaId) }
    }

    fun pruneOrphanedMemberships(validMediaIds: Set<Long>) {
        viewModelScope.launch { repository.pruneOrphanedMemberships(validMediaIds) }
    }
}

data class PetWithPreview(
    val id: Long,
    val name: String,
    val itemCount: Int,
    val previewItems: List<MediaItem>
)
