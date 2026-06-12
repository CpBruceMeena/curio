package com.curio.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OfflineNovelChapterDao {

    @Query("SELECT * FROM offline_novel_chapters WHERE novelId = :novelId ORDER BY chapterNumber ASC")
    suspend fun getChapters(novelId: Long): List<OfflineNovelChapter>

    @Query("SELECT * FROM offline_novel_chapters WHERE novelId = :novelId AND chapterNumber = :chapterNum")
    suspend fun getChapter(novelId: Long, chapterNum: Int): OfflineNovelChapter?

    @Query("SELECT COUNT(*) FROM offline_novel_chapters WHERE novelId = :novelId")
    suspend fun getDownloadedCount(novelId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chapter: OfflineNovelChapter)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<OfflineNovelChapter>)

    @Query("DELETE FROM offline_novel_chapters WHERE novelId = :novelId")
    suspend fun deleteByNovelId(novelId: Long)
}
