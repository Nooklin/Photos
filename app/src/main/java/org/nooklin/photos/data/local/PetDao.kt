package org.nooklin.photos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pets ORDER BY name COLLATE NOCASE")
    fun observePets(): Flow<List<PetEntity>>

    @Query("SELECT * FROM pet_membership")
    fun observeMemberships(): Flow<List<PetMembershipEntity>>

    @Insert
    suspend fun insertPet(pet: PetEntity): Long

    @Query("UPDATE pets SET name = :newName WHERE id = :petId")
    suspend fun renamePet(petId: Long, newName: String)

    @Query("DELETE FROM pets WHERE id = :petId")
    suspend fun deletePet(petId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMembership(membership: PetMembershipEntity)

    @Query("DELETE FROM pet_membership WHERE petId = :petId AND mediaId = :mediaId")
    suspend fun removeMembership(petId: Long, mediaId: Long)

    @Query("SELECT DISTINCT mediaId FROM pet_membership")
    suspend fun distinctMemberMediaIds(): List<Long>

    @Query("DELETE FROM pet_membership WHERE mediaId IN (:mediaIds)")
    suspend fun deleteMembershipsForMediaIds(mediaIds: List<Long>)
}
