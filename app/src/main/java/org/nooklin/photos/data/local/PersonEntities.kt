package org.nooklin.photos.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// Unnamed people are labeled "Unknown N" at display time, a stored number would collide or leave gaps
@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val createdAt: Long
)

// One row per face, embedding is a String, see FaceClusterer
@Entity(tableName = "face_embeddings", indices = [Index("personId"), Index("mediaId")])
data class FaceEmbeddingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: Long,
    val faceIndex: Int,
    val personId: Long,
    val embedding: String,
    val confidence: Float,
    val boxXMin: Float,
    val boxYMin: Float,
    val boxXMax: Float,
    val boxYMax: Float
)

data class FaceSummary(
    val id: Long,
    val mediaId: Long,
    val personId: Long,
    val confidence: Float,
    val boxXMin: Float,
    val boxYMin: Float,
    val boxXMax: Float,
    val boxYMax: Float
)

@Dao
interface PersonDao {
    @Insert
    suspend fun insertPerson(person: PersonEntity): Long

    @Query("SELECT * FROM people")
    fun observeAllPeople(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun personById(id: Long): PersonEntity?

    @Query("SELECT * FROM people")
    suspend fun peopleSnapshot(): List<PersonEntity>

    // "mom" and "Mom" are the same person
    @Query("SELECT * FROM people WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): PersonEntity?

    @Query("UPDATE people SET name = :name WHERE id = :id")
    suspend fun renamePerson(id: Long, name: String?)

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun deletePerson(id: Long)

    @Insert
    suspend fun insertFaceEmbedding(embedding: FaceEmbeddingEntity): Long

    @Query("SELECT * FROM face_embeddings")
    fun observeAllFaceEmbeddings(): Flow<List<FaceEmbeddingEntity>>

    @Query("SELECT id, mediaId, personId, confidence, boxXMin, boxYMin, boxXMax, boxYMax FROM face_embeddings")
    fun observeAllFaceSummaries(): Flow<List<FaceSummary>>

    @Query("SELECT * FROM face_embeddings")
    suspend fun allFaceEmbeddingsSnapshot(): List<FaceEmbeddingEntity>

    @Query("SELECT * FROM face_embeddings WHERE mediaId = :mediaId")
    suspend fun facesForMedia(mediaId: Long): List<FaceEmbeddingEntity>

    // Re-analysis must not pile up duplicate faces
    @Query("DELETE FROM face_embeddings WHERE mediaId = :mediaId")
    suspend fun clearFacesForMedia(mediaId: Long)

    @Query("SELECT DISTINCT mediaId FROM face_embeddings")
    suspend fun distinctFaceMediaIds(): List<Long>

    @Query("DELETE FROM face_embeddings WHERE mediaId IN (:mediaIds)")
    suspend fun deleteFacesForMediaIds(mediaIds: List<Long>)

    @Query("UPDATE face_embeddings SET personId = :newPersonId WHERE personId = :oldPersonId")
    suspend fun reassignAllFaces(oldPersonId: Long, newPersonId: Long)

    @Query("UPDATE face_embeddings SET personId = :newPersonId WHERE id = :faceId")
    suspend fun reassignFace(faceId: Long, newPersonId: Long)

    @Transaction
    suspend fun mergePeople(intoPersonId: Long, fromPersonId: Long) {
        reassignAllFaces(fromPersonId, intoPersonId)
        deletePerson(fromPersonId)
    }

    @Query("DELETE FROM face_embeddings")
    suspend fun clearAllFaceEmbeddings()

    @Query("DELETE FROM people")
    suspend fun clearAllPeople()

    @Transaction
    suspend fun clearAll() {
        clearAllFaceEmbeddings()
        clearAllPeople()
    }
}
