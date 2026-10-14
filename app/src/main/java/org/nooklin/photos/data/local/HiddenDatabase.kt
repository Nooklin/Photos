package org.nooklin.photos.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Flags are captured at hide-time, a hidden file has no MediaStore row
@Entity(tableName = "hidden_media")
data class HiddenMediaEntity(
    @PrimaryKey val fileName: String,
    val isVideo: Boolean,
    val hiddenAt: Long,
    val isFavorite: Boolean = false,
    val isScreenshot: Boolean = false,
    val isSlowMotion: Boolean = false,
    val isRaw: Boolean = false,
    val isPanorama: Boolean = false,
    val isScreenRecording: Boolean = false,
    val width: Int? = null,
    val height: Int? = null,
    val mimeType: String? = null,
    // Library date (seconds) at hide-time, so unhide can put the item back in its old place. Null for items hidden before v1.1.0.
    val dateAdded: Long? = null,
    // Set when deleted from the Hidden Section. The file stays private until purged after 30 days.
    val deletedAt: Long? = null
)

@Entity(tableName = "hidden_albums")
data class HiddenAlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "hidden_album_membership",
    primaryKeys = ["albumId", "fileName"],
    foreignKeys = [
        ForeignKey(
            entity = HiddenAlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = HiddenMediaEntity::class,
            parentColumns = ["fileName"],
            childColumns = ["fileName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("albumId"), Index("fileName")]
)
data class HiddenAlbumMembershipEntity(
    val albumId: Long,
    val fileName: String
)

@Dao
interface HiddenDao {
    @Query("SELECT * FROM hidden_media")
    suspend fun allMedia(): List<HiddenMediaEntity>

    @Query("UPDATE hidden_media SET deletedAt = :deletedAt WHERE fileName = :fileName")
    suspend fun setDeletedAt(fileName: String, deletedAt: Long?)

    @Query("SELECT * FROM hidden_media WHERE fileName = :fileName")
    suspend fun mediaFor(fileName: String): HiddenMediaEntity?

    // IGNORE not REPLACE: REPLACE would cascade-delete album memberships
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMedia(entity: HiddenMediaEntity)

    @Query("UPDATE hidden_media SET isFavorite = :favorite WHERE fileName = :fileName")
    suspend fun setFavorite(fileName: String, favorite: Boolean)

    @Query("UPDATE hidden_media SET width = :width, height = :height WHERE fileName = :fileName")
    suspend fun setSize(fileName: String, width: Int, height: Int)

    @Query("UPDATE hidden_media SET mimeType = :mimeType WHERE fileName = :fileName")
    suspend fun setMimeType(fileName: String, mimeType: String)

    @Query("DELETE FROM hidden_media WHERE fileName = :fileName")
    suspend fun deleteMedia(fileName: String)

    @Query("SELECT * FROM hidden_albums")
    suspend fun allAlbums(): List<HiddenAlbumEntity>

    @Insert
    suspend fun insertAlbum(album: HiddenAlbumEntity): Long

    @Query("UPDATE hidden_albums SET name = :newName WHERE id = :albumId")
    suspend fun renameAlbum(albumId: Long, newName: String)

    @Query("DELETE FROM hidden_albums WHERE id = :albumId")
    suspend fun deleteAlbum(albumId: Long)

    @Query("SELECT * FROM hidden_album_membership")
    suspend fun allMemberships(): List<HiddenAlbumMembershipEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMembership(membership: HiddenAlbumMembershipEntity)

    @Query("DELETE FROM hidden_album_membership WHERE albumId = :albumId AND fileName = :fileName")
    suspend fun removeMembership(albumId: Long, fileName: String)
}

// Own database, nothing of a hidden item stays in NooklinDatabase. Not encrypted yet.
@Database(
    entities = [
        HiddenMediaEntity::class,
        HiddenAlbumEntity::class,
        HiddenAlbumMembershipEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class HiddenDatabase : RoomDatabase() {
    abstract fun hiddenDao(): HiddenDao

    companion object {
        @Volatile
        private var INSTANCE: HiddenDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hidden_media ADD COLUMN dateAdded INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hidden_media ADD COLUMN deletedAt INTEGER")
            }
        }

        fun getInstance(context: Context): HiddenDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HiddenDatabase::class.java,
                    "nooklin_hidden.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}
