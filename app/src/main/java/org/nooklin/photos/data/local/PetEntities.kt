package org.nooklin.photos.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Manual pets only, no clustering. Own table so pets and albums stay separate.
@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "pet_membership",
    primaryKeys = ["petId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("petId"), Index("mediaId")]
)
data class PetMembershipEntity(
    val petId: Long,
    val mediaId: Long
)
