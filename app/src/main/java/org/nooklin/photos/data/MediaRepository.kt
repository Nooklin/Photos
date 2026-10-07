package org.nooklin.photos.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.nooklin.photos.data.local.LocalMetadataStore

class MediaRepository(
    private val mediaStoreRepository: MediaStoreRepository,
    private val localMetadataStore: LocalMetadataStore,
    private val originals: OriginalsStore? = null
) {
    suspend fun loadAll(): List<MediaItem> {
        val raw = mediaStoreRepository.queryMedia()
        val favoriteIds = localMetadataStore.favoriteIds.first()
        val recycledIds = localMetadataStore.recycleBinEntries.first().map { it.mediaId }.toSet()
        val sortDates = localMetadataStore.sortDates.first()
        // Drop stored originals of vanished photos.
        // Skipped on an empty query, that is more likely a denied query.
        if (raw.isNotEmpty() && originals != null) {
            val liveIds = raw.mapTo(HashSet()) { it.id }
            withContext(Dispatchers.IO) { originals.pruneMedia(liveIds) }
        }
        return raw
            .filterNot { it.id in recycledIds }
            .map { it.copy(isFavorite = it.id in favoriteIds, dateAdded = sortDates[it.id] ?: it.dateAdded) }
            // Sorted again, a date override can move a photo
            .sortedByDescending { it.dateAdded }
    }

    suspend fun toggleFavorite(item: MediaItem) {
        localMetadataStore.setFavorite(item.id, !item.isFavorite)
    }

    suspend fun moveToRecycleBin(item: MediaItem) {
        localMetadataStore.moveToRecycleBin(item.id, item.uri.toString(), width = item.width, height = item.height)
    }

    suspend fun restoreFromRecycleBin(mediaId: Long) {
        localMetadataStore.restoreFromRecycleBin(mediaId)
    }

    fun observeRecycleBin(): Flow<List<LocalMetadataStore.RecycleBinEntry>> =
        localMetadataStore.recycleBinEntries

    suspend fun findById(mediaId: Long): MediaItem? = mediaStoreRepository.queryMediaById(mediaId)
}
