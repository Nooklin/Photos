package org.nooklin.photos.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// One row per detected thing, a photo can hold several
@Entity(tableName = "photo_tags", primaryKeys = ["mediaId", "tag"])
data class PhotoTagEntity(
    val mediaId: Long,
    val tag: String,
    val confidence: Float
)

// One row per analyzed photo, so "analyzed, found nothing" differs from "never analyzed"
@Entity(tableName = "photo_analysis_status")
data class PhotoAnalysisStatusEntity(
    @PrimaryKey val mediaId: Long,
    val dateModified: Long,
    val analyzedAt: Long
)

data class AnalysisStatusDate(val mediaId: Long, val dateModified: Long)

@Dao
interface PhotoAnalysisDao {
    @Query("SELECT * FROM photo_tags")
    fun observeAllTags(): Flow<List<PhotoTagEntity>>

    @Query("SELECT * FROM photo_tags WHERE mediaId = :mediaId")
    suspend fun tagsFor(mediaId: Long): List<PhotoTagEntity>

    @Query("SELECT mediaId, dateModified FROM photo_analysis_status")
    suspend fun allStatusDates(): List<AnalysisStatusDate>

    @Query("SELECT * FROM photo_analysis_status WHERE mediaId = :mediaId")
    suspend fun statusFor(mediaId: Long): PhotoAnalysisStatusEntity?

    @Query("DELETE FROM photo_tags WHERE mediaId = :mediaId")
    suspend fun clearTagsFor(mediaId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<PhotoTagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatus(status: PhotoAnalysisStatusEntity)

    // Replaces the whole tag set, re-analysis must not leave stale rows
    @Transaction
    suspend fun saveAnalysis(mediaId: Long, dateModified: Long, tags: List<PhotoTagEntity>) {
        clearTagsFor(mediaId)
        if (tags.isNotEmpty()) insertTags(tags)
        upsertStatus(PhotoAnalysisStatusEntity(mediaId, dateModified, System.currentTimeMillis()))
    }

    @Query("SELECT COUNT(*) FROM photo_analysis_status")
    suspend fun analyzedCount(): Int

    @Query("SELECT DISTINCT mediaId FROM photo_tags WHERE tag IN (:tags)")
    suspend fun mediaIdsForTags(tags: List<String>): List<Long>

    @Query("DELETE FROM photo_tags WHERE tag IN (:tags)")
    suspend fun deleteTags(tags: List<String>)

    @Query("DELETE FROM photo_analysis_status WHERE mediaId IN (:mediaIds)")
    suspend fun clearStatusForMediaIds(mediaIds: List<Long>)

    // Also resets status, else photos stay "analyzed" with the tag gone
    @Transaction
    suspend fun clearCategories(tags: List<String>) {
        val affected = mediaIdsForTags(tags)
        deleteTags(tags)
        if (affected.isNotEmpty()) clearStatusForMediaIds(affected)
    }

    @Query("DELETE FROM photo_tags")
    suspend fun clearAllTags()

    @Query("DELETE FROM photo_analysis_status")
    suspend fun clearAllStatus()

    @Transaction
    suspend fun clearAll() {
        clearAllTags()
        clearAllStatus()
        clearAllHashes()
    }

    @Query("SELECT DISTINCT mediaId FROM photo_tags")
    suspend fun distinctTaggedMediaIds(): List<Long>

    @Query("SELECT mediaId FROM photo_analysis_status")
    suspend fun allStatusMediaIds(): List<Long>

    @Query("DELETE FROM photo_tags WHERE mediaId IN (:mediaIds)")
    suspend fun deleteTagsForMediaIds(mediaIds: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHash(hash: PhotoHashEntity)

    @Query("SELECT * FROM photo_hashes")
    suspend fun allHashes(): List<PhotoHashEntity>

    @Query("DELETE FROM photo_hashes WHERE mediaId IN (:mediaIds)")
    suspend fun deleteHashesForMediaIds(mediaIds: List<Long>)

    @Query("DELETE FROM photo_hashes")
    suspend fun clearAllHashes()
}

// dHash per photo, own table because it is a scalar, not a tag
@Entity(tableName = "photo_hashes")
data class PhotoHashEntity(
    @PrimaryKey val mediaId: Long,
    val dHash: Long
)
