package org.nooklin.photos.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    // Set for a folder: the id of the album it sits in. Null for a normal album. One level only.
    val parentId: Long? = null,
    // The photo chosen for the top of the album screen. Null: the newest photo.
    val coverMediaId: Long? = null
)

// mediaId is a MediaStore id, not a Room foreign key
@Entity(
    tableName = "album_membership",
    primaryKeys = ["albumId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("albumId"), Index("mediaId")]
)
data class AlbumMembershipEntity(
    val albumId: Long,
    val mediaId: Long
)
