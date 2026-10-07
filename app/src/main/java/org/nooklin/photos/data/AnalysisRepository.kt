package org.nooklin.photos.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nooklin.photos.data.local.NooklinDatabase
import org.nooklin.photos.data.local.PhotoHashEntity
import org.nooklin.photos.data.local.PhotoTagEntity
import org.nooklin.photos.ml.hammingDistance

class AnalysisRepository(context: Context) {
    private val dao = NooklinDatabase.getInstance(context).photoAnalysisDao()

    val allTags = dao.observeAllTags()

    suspend fun tagsFor(mediaId: Long): List<PhotoTagEntity> = withContext(Dispatchers.IO) {
        dao.tagsFor(mediaId)
    }

    suspend fun needsAnalysis(mediaId: Long, currentDateModified: Long): Boolean = withContext(Dispatchers.IO) {
        val status = dao.statusFor(mediaId)
        status == null || status.dateModified != currentDateModified
    }

    suspend fun saveAnalysis(mediaId: Long, dateModified: Long, tags: List<Pair<String, Float>>) = withContext(Dispatchers.IO) {
        dao.saveAnalysis(
            mediaId,
            dateModified,
            tags.map { (tag, confidence) -> PhotoTagEntity(mediaId, tag, confidence) }
        )
    }

    suspend fun statusDates(): Map<Long, Long> = withContext(Dispatchers.IO) {
        dao.allStatusDates().associate { it.mediaId to it.dateModified }
    }

    suspend fun analyzedCount(): Int = withContext(Dispatchers.IO) { dao.analyzedCount() }

    suspend fun saveHash(mediaId: Long, dHash: Long) = withContext(Dispatchers.IO) {
        dao.upsertHash(PhotoHashEntity(mediaId, dHash))
    }

    suspend fun findDuplicateMediaIds(validMediaIds: Set<Long>): Set<Long> = withContext(Dispatchers.IO) {
        // Flat images give all-0 or all-1 hashes that match each other
        val hashes = dao.allHashes().filter { it.mediaId in validMediaIds && it.dHash != 0L && it.dHash != -1L }
        val duplicateIds = mutableSetOf<Long>()
        // Within 5 bits means one of 6 chunks is equal, so compare same-chunk pairs only
        var shift = 0
        for (bits in intArrayOf(11, 11, 11, 11, 10, 10)) {
            val mask = (1L shl bits) - 1
            for (bucket in hashes.groupBy { (it.dHash ushr shift) and mask }.values) {
                for (i in bucket.indices) {
                    for (j in i + 1 until bucket.size) {
                        if (hammingDistance(bucket[i].dHash, bucket[j].dHash) <= HAMMING_THRESHOLD) {
                            duplicateIds.add(bucket[i].mediaId)
                            duplicateIds.add(bucket[j].mediaId)
                        }
                    }
                }
            }
            shift += bits
        }
        duplicateIds
    }

    private companion object {
        // Out of 64 bits, picked by judgment
        const val HAMMING_THRESHOLD = 5
    }

    suspend fun clearCategories(tagIds: Set<String>) = withContext(Dispatchers.IO) {
        dao.clearCategories(tagIds.toList())
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) { dao.clearAll() }

    // A stale status row makes a reused id look already scanned
    suspend fun pruneOrphanedTags(validMediaIds: Set<Long>) = withContext(Dispatchers.IO) {
        val referencedTags = dao.distinctTaggedMediaIds()
        val orphanedTags = referencedTags.filterNot { it in validMediaIds }
        if (orphanedTags.isNotEmpty()) dao.deleteTagsForMediaIds(orphanedTags)

        val referencedStatus = dao.allStatusMediaIds()
        val orphanedStatus = referencedStatus.filterNot { it in validMediaIds }
        if (orphanedStatus.isNotEmpty()) dao.clearStatusForMediaIds(orphanedStatus)

        val referencedHashes = dao.allHashes().map { it.mediaId }
        val orphanedHashes = referencedHashes.filterNot { it in validMediaIds }
        if (orphanedHashes.isNotEmpty()) dao.deleteHashesForMediaIds(orphanedHashes)
    }
}
