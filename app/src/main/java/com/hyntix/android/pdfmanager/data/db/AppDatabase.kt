package com.hyntix.android.pdfmanager.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hyntix.android.pdfmanager.data.model.CachedPdfFile
import com.hyntix.android.pdfmanager.data.model.FavoriteFile
import com.hyntix.android.pdfmanager.data.model.Folder
import com.hyntix.android.pdfmanager.data.model.FolderPdfCrossRef
import com.hyntix.android.pdfmanager.data.model.RecentFile

@Database(
    entities = [
        RecentFile::class,
        FavoriteFile::class,
        CachedPdfFile::class,
        Folder::class,
        FolderPdfCrossRef::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recentFileDao(): RecentFileDao
    abstract fun favoriteFileDao(): FavoriteFileDao
    abstract fun cachedPdfFileDao(): CachedPdfFileDao
    abstract fun folderDao(): FolderDao
    abstract fun folderPdfCrossRefDao(): FolderPdfCrossRefDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pdf_viewer_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `favorite_files` (" +
                    "`uri` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`sizeBytes` INTEGER NOT NULL, " +
                    "`addedAt` INTEGER NOT NULL, " +  // Fixed: was addedTimestamp, must match entity field name
                    "PRIMARY KEY(`uri`))"
        )
    }
}

val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE recent_files ADD COLUMN lastPage INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        // Create cached_pdf_files table for instant file list loading
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cached_pdf_files` (
                `path` TEXT NOT NULL PRIMARY KEY,
                `name` TEXT NOT NULL,
                `size` INTEGER NOT NULL,
                `lastModified` INTEGER NOT NULL,
                `uri` TEXT NOT NULL,
                `lastScanned` INTEGER NOT NULL DEFAULT 0
            )
        """)
        // Create indices for faster queries
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_pdf_files_lastModified` ON `cached_pdf_files` (`lastModified`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_pdf_files_lastScanned` ON `cached_pdf_files` (`lastScanned`)")
    }
}

val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        // Create folders table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `folders` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
        """)
        
        // Create folder_pdf_cross_ref junction table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `folder_pdf_cross_ref` (
                `folderId` INTEGER NOT NULL,
                `pdfUri` TEXT NOT NULL,
                `addedAt` INTEGER NOT NULL,
                PRIMARY KEY(`folderId`, `pdfUri`),
                FOREIGN KEY(`folderId`) REFERENCES `folders`(`id`) ON DELETE CASCADE
            )
        """)
        
        // Create indices for efficient lookups
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_folder_pdf_cross_ref_folderId` ON `folder_pdf_cross_ref` (`folderId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_folder_pdf_cross_ref_pdfUri` ON `folder_pdf_cross_ref` (`pdfUri`)")
    }
}

