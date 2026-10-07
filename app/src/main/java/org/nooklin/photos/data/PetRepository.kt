package org.nooklin.photos.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nooklin.photos.data.local.NooklinDatabase
import org.nooklin.photos.data.local.PetEntity
import org.nooklin.photos.data.local.PetMembershipEntity

class PetRepository(context: Context) {
    private val dao = NooklinDatabase.getInstance(context).petDao()

    val pets = dao.observePets()
    val memberships = dao.observeMemberships()

    suspend fun createPet(name: String): Long = withContext(Dispatchers.IO) {
        dao.insertPet(PetEntity(name = name))
    }

    suspend fun renamePet(petId: Long, newName: String) = withContext(Dispatchers.IO) {
        dao.renamePet(petId, newName)
    }

    suspend fun deletePet(petId: Long) = withContext(Dispatchers.IO) {
        dao.deletePet(petId)
    }

    suspend fun addToPet(petId: Long, mediaId: Long) = withContext(Dispatchers.IO) {
        dao.insertMembership(PetMembershipEntity(petId, mediaId))
    }

    suspend fun removeFromPet(petId: Long, mediaId: Long) = withContext(Dispatchers.IO) {
        dao.removeMembership(petId, mediaId)
    }

    suspend fun pruneOrphanedMemberships(validMediaIds: Set<Long>) = withContext(Dispatchers.IO) {
        val referenced = dao.distinctMemberMediaIds()
        val orphaned = referenced.filterNot { it in validMediaIds }
        if (orphaned.isNotEmpty()) dao.deleteMembershipsForMediaIds(orphaned)
    }
}
