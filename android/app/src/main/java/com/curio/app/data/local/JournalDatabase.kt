package com.curio.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        JournalEntry::class,
        BookmarkedContent::class,
        CachedContent::class,
        OfflineNovel::class,
        OfflineNovelChapter::class,
        LocalNovelProgress::class
    ],
    version = 4,
    exportSchema = false
)
abstract class JournalDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao
    abstract fun bookmarkedContentDao(): BookmarkedContentDao
    abstract fun cachedContentDao(): CachedContentDao
    abstract fun offlineNovelDao(): OfflineNovelDao
    abstract fun offlineNovelChapterDao(): OfflineNovelChapterDao
    abstract fun localNovelProgressDao(): LocalNovelProgressDao

    companion object {
        @Volatile
        private var INSTANCE: JournalDatabase? = null

        fun getInstance(context: Context): JournalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    JournalDatabase::class.java,
                    "curio_journal.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
