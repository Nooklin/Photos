package org.nooklin.photos.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import org.nooklin.photos.data.local.AlbumEntity
import org.nooklin.photos.data.local.AppPreferences
import org.nooklin.photos.data.local.AlbumMembershipEntity
import org.nooklin.photos.data.local.NooklinDatabase

class AlbumRepository(context: Context) {
    private val dao = NooklinDatabase.getInstance(context).albumDao()
    private val preferences = AppPreferences(context)

    val albums = dao.observeAlbums()
    val memberships = dao.observeMemberships()

    // Seeds the Camera folder as a starting album
    suspend fun seedFromFoldersIfNeeded(items: List<MediaItem>) = withContext(Dispatchers.IO) {
        // Once ever, deleted albums must not come back
        if (preferences.albumsSeeded.first()) return@withContext
        if (dao.albumCount() > 0) {
            preferences.setAlbumsSeeded(true)
            return@withContext
        }
        // Only the Camera folder becomes an album
        val byBucket = items.filter { it.bucketId != null && it.bucketName.equals("Camera", ignoreCase = true) }.groupBy { it.bucketId to it.bucketName }
        for ((key, groupItems) in byBucket) {
            val albumId = dao.insertAlbum(AlbumEntity(name = key.second ?: "Unknown"))
            dao.insertMemberships(groupItems.map { AlbumMembershipEntity(albumId, it.id) })
        }
        preferences.setAlbumsSeeded(true)
    }

    suspend fun createAlbum(name: String): Long = withContext(Dispatchers.IO) {
        dao.insertAlbum(AlbumEntity(name = name))
    }

    suspend fun renameAlbum(albumId: Long, newName: String) = withContext(Dispatchers.IO) {
        dao.renameAlbum(albumId, newName)
    }

    suspend fun deleteAlbum(albumId: Long) = withContext(Dispatchers.IO) {
        dao.deleteAlbum(albumId)
    }

    suspend fun addToAlbum(albumId: Long, mediaId: Long) = withContext(Dispatchers.IO) {
        dao.insertMembership(AlbumMembershipEntity(albumId, mediaId))
    }

    suspend fun removeFromAlbum(albumId: Long, mediaId: Long) = withContext(Dispatchers.IO) {
        dao.removeMembership(albumId, mediaId)
    }

    suspend fun moveBetweenAlbums(fromAlbumId: Long, toAlbumId: Long, mediaId: Long) = withContext(Dispatchers.IO) {
        dao.removeMembership(fromAlbumId, mediaId)
        dao.insertMembership(AlbumMembershipEntity(toAlbumId, mediaId))
    }

    suspend fun mergeAlbums(sourceAlbumId: Long, targetAlbumId: Long) = withContext(Dispatchers.IO) {
        dao.mergeAlbums(sourceAlbumId, targetAlbumId)
    }

    // MediaStore ids can be reused, so drop memberships of deleted photos
    suspend fun pruneOrphanedMemberships(validMediaIds: Set<Long>) = withContext(Dispatchers.IO) {
        val referenced = dao.distinctMemberMediaIds()
        val orphaned = referenced.filterNot { it in validMediaIds }
        if (orphaned.isNotEmpty()) dao.deleteMembershipsForMediaIds(orphaned)
    }
}
