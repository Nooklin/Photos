package org.nooklin.photos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY name COLLATE NOCASE")
    fun observeAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM album_membership")
    fun observeMemberships(): Flow<List<AlbumMembershipEntity>>

    @Query("SELECT COUNT(*) FROM albums")
    suspend fun albumCount(): Int

    @Insert
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Query("UPDATE albums SET coverMediaId = :mediaId WHERE id = :albumId")
    suspend fun setCover(albumId: Long, mediaId: Long?)

    @Query("UPDATE albums SET name = :newName WHERE id = :albumId")
    suspend fun renameAlbum(albumId: Long, newName: String)

    @Query("DELETE FROM albums WHERE id = :albumId")
    suspend fun deleteAlbumRow(albumId: Long)

    // The memberships of a deleted album or folder go with the cascade, the photos themselves are never touched
    @Query("DELETE FROM albums WHERE parentId = :albumId")
    suspend fun deleteFoldersOf(albumId: Long)

    @Query("SELECT parentId FROM albums WHERE id = :albumId")
    suspend fun parentOf(albumId: Long): Long?

    // A deleted folder gives its photos to the album it sat in
    @Transaction
    suspend fun deleteAlbum(albumId: Long) {
        parentOf(albumId)?.let { reassignMembership(albumId, it) }
        deleteFoldersOf(albumId)
        deleteAlbumRow(albumId)
    }

    @Query("UPDATE albums SET parentId = :targetAlbumId WHERE parentId = :sourceAlbumId")
    suspend fun moveFolders(sourceAlbumId: Long, targetAlbumId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMembership(membership: AlbumMembershipEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMemberships(memberships: List<AlbumMembershipEntity>)

    @Query("DELETE FROM album_membership WHERE albumId = :albumId AND mediaId = :mediaId")
    suspend fun removeMembership(albumId: Long, mediaId: Long)

    @Query("SELECT DISTINCT mediaId FROM album_membership")
    suspend fun distinctMemberMediaIds(): List<Long>

    @Query("DELETE FROM album_membership WHERE mediaId IN (:mediaIds)")
    suspend fun deleteMembershipsForMediaIds(mediaIds: List<Long>)

    // OR IGNORE: if the photo is already in the target, the leftover row goes with deleteAlbum's cascade
    @Query("UPDATE OR IGNORE album_membership SET albumId = :targetAlbumId WHERE albumId = :sourceAlbumId")
    suspend fun reassignMembership(sourceAlbumId: Long, targetAlbumId: Long)

    @Transaction
    suspend fun mergeAlbums(sourceAlbumId: Long, targetAlbumId: Long) {
        reassignMembership(sourceAlbumId, targetAlbumId)
        // The folders of the merged album move to the target, their photos stay in them
        moveFolders(sourceAlbumId, targetAlbumId)
        deleteAlbum(sourceAlbumId)
    }
}
