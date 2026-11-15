package org.nooklin.photos.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.nooklin.photos.data.local.AlbumEntity
import org.nooklin.photos.data.local.AppPreferences
import org.nooklin.photos.data.local.AlbumMembershipEntity
import org.nooklin.photos.data.local.NooklinDatabase

private class AutoAlbumRule(val key: String, val name: String, val matches: (MediaItem) -> Boolean)

private val autoRules = listOf(
    AutoAlbumRule("camera", "Camera") { it.bucketName.equals("Camera", ignoreCase = true) },
    AutoAlbumRule("screenshots", "Screenshots") { it.isScreenshot },
    AutoAlbumRule("downloads", "Downloads") {
        it.bucketName.equals("Download", ignoreCase = true) || it.bucketName.equals("Downloads", ignoreCase = true)
    },
    AutoAlbumRule("whatsapp", "WhatsApp") { it.bucketName?.startsWith("WhatsApp", ignoreCase = true) == true }
)

class AlbumRepository(context: Context) {
    private val dao = NooklinDatabase.getInstance(context).albumDao()
    private val preferences = AppPreferences(context)

    val albums = dao.observeAlbums()
    val memberships = dao.observeMemberships()

    // rule key (camera, screenshots, ...) to the album that follows it
    val autoAlbums: Flow<Map<String, Long>> = preferences.autoAlbums.map { entries ->
        entries.mapNotNull { entry ->
            entry.substringAfter(':', "").toLongOrNull()?.let { entry.substringBefore(':') to it }
        }.toMap()
    }

    val hiddenAutoAlbumIds: Flow<Set<Long>> = combine(autoAlbums, preferences.hiddenAutoAlbums) { ids, hidden ->
        ids.filterKeys { it in hidden }.values.toSet()
    }

    // Albums that follow a MediaStore folder: new photos in the folder join them on every library load.
    // Album ids are remembered, so an album the user deletes does not come back.
    suspend fun syncAutoAlbums(items: List<MediaItem>) = withContext(Dispatchers.IO) {
        val known = preferences.autoAlbums.first()
        val albumIds = known.mapNotNull { entry ->
            entry.substringAfter(':', "").toLongOrNull()?.let { entry.substringBefore(':') to it }
        }.toMap().toMutableMap()
        var changed = false
        for (rule in autoRules) {
            val matching = items.filter(rule.matches)
            if (matching.isEmpty()) continue
            val existing = albumIds[rule.key]
            val albumId: Long
            if (existing == null) {
                albumId = dao.topLevelAlbumIdByName(rule.name) ?: dao.insertAlbum(AlbumEntity(name = rule.name))
                albumIds[rule.key] = albumId
                changed = true
            } else if (dao.albumExists(existing) == 0) {
                continue
            } else {
                albumId = existing
            }
            val have = dao.memberIds(albumId).toHashSet()
            val missing = matching.filter { it.id !in have }
            if (missing.isNotEmpty()) dao.insertMemberships(missing.map { AlbumMembershipEntity(albumId, it.id) })
        }
        if (changed) preferences.setAutoAlbums(albumIds.map { "${it.key}:${it.value}" }.toSet())
    }

    suspend fun createAlbum(name: String): Long = withContext(Dispatchers.IO) {
        dao.insertAlbum(AlbumEntity(name = name))
    }

    suspend fun createFolder(parentId: Long, name: String): Long = withContext(Dispatchers.IO) {
        dao.insertAlbum(AlbumEntity(name = name, parentId = parentId))
    }

    suspend fun renameAlbum(albumId: Long, newName: String) = withContext(Dispatchers.IO) {
        dao.renameAlbum(albumId, newName)
    }

    suspend fun setCover(albumId: Long, mediaId: Long?) = withContext(Dispatchers.IO) {
        dao.setCover(albumId, mediaId)
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

    suspend fun pruneOrphanedMemberships(validMediaIds: Set<Long>) = withContext(Dispatchers.IO) {
        val referenced = dao.distinctMemberMediaIds()
        val orphaned = referenced.filterNot { it in validMediaIds }
        if (orphaned.isNotEmpty()) dao.deleteMembershipsForMediaIds(orphaned)
    }
}
