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
    val parentId: Long? = null,
    val coverMediaId: Long? = null
)

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
