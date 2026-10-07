package org.nooklin.photos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AlbumEntity::class,
        AlbumMembershipEntity::class,
        PhotoTagEntity::class,
        PhotoAnalysisStatusEntity::class,
        PersonEntity::class,
        FaceEmbeddingEntity::class,
        PetEntity::class,
        PetMembershipEntity::class,
        PhotoHashEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class NooklinDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun photoAnalysisDao(): PhotoAnalysisDao
    abstract fun personDao(): PersonDao
    abstract fun petDao(): PetDao

    companion object {
        @Volatile
        private var INSTANCE: NooklinDatabase? = null

        // Real migrations, not destructive: albums already hold data
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `photo_analysis` (
                        `mediaId` INTEGER NOT NULL,
                        `dateModified` INTEGER NOT NULL,
                        `faceCount` INTEGER,
                        `petType` TEXT,
                        `analyzedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`mediaId`)
                    )
                    """.trimIndent()
                )
            }
        }

        // The v2 table never held real data, safe to drop
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `photo_analysis`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `photo_tags` (
                        `mediaId` INTEGER NOT NULL,
                        `tag` TEXT NOT NULL,
                        `confidence` REAL NOT NULL,
                        PRIMARY KEY(`mediaId`, `tag`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `photo_analysis_status` (
                        `mediaId` INTEGER NOT NULL,
                        `dateModified` INTEGER NOT NULL,
                        `analyzedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`mediaId`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `people` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `face_embeddings` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `mediaId` INTEGER NOT NULL,
                        `faceIndex` INTEGER NOT NULL,
                        `personId` INTEGER NOT NULL,
                        `embedding` TEXT NOT NULL,
                        `confidence` REAL NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_face_embeddings_personId` ON `face_embeddings` (`personId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_face_embeddings_mediaId` ON `face_embeddings` (`mediaId`)")
            }
        }

        // Drops people and face_embeddings, no real data existed yet
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `face_embeddings`")
                db.execSQL("DROP TABLE IF EXISTS `people`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `people` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `face_embeddings` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `mediaId` INTEGER NOT NULL,
                        `faceIndex` INTEGER NOT NULL,
                        `personId` INTEGER NOT NULL,
                        `embedding` TEXT NOT NULL,
                        `confidence` REAL NOT NULL,
                        `boxXMin` REAL NOT NULL,
                        `boxYMin` REAL NOT NULL,
                        `boxXMax` REAL NOT NULL,
                        `boxYMax` REAL NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_face_embeddings_personId` ON `face_embeddings` (`personId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_face_embeddings_mediaId` ON `face_embeddings` (`mediaId`)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pet_membership` (
                        `petId` INTEGER NOT NULL,
                        `mediaId` INTEGER NOT NULL,
                        PRIMARY KEY(`petId`, `mediaId`),
                        FOREIGN KEY(`petId`) REFERENCES `pets`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pet_membership_petId` ON `pet_membership` (`petId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pet_membership_mediaId` ON `pet_membership` (`mediaId`)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `photo_hashes` (
                        `mediaId` INTEGER NOT NULL,
                        `dHash` INTEGER NOT NULL,
                        PRIMARY KEY(`mediaId`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): NooklinDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NooklinDatabase::class.java,
                    "nooklin_albums.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build().also { INSTANCE = it }
            }
    }
}
