package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MuezzinEntity::class, DhikrEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun muezzinDao(): MuezzinDao
    abstract fun dhikrDao(): DhikrDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dhikr_items` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `titleAr` TEXT NOT NULL,
                        `textAr` TEXT NOT NULL,
                        `categoryAr` TEXT NOT NULL,
                        `reciterAr` TEXT NOT NULL,
                        `audioUrl` TEXT NOT NULL,
                        `fallbackAudioUrl` TEXT NOT NULL,
                        `localFilePath` TEXT,
                        `isDownloaded` INTEGER NOT NULL,
                        `isDownloading` INTEGER NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `isCustom` INTEGER NOT NULL,
                        `lastPlayedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "noor_database"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
        }
    }
}
