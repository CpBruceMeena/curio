package com.curio.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalNovelProgressDao {

    @Query("SELECT * FROM local_novel_progress WHERE novelId = :novelId")
    suspend fun getProgress(novelId: Long): LocalNovelProgress?

    @Query("SELECT * FROM local_novel_progress WHERE bookmarked = 1 ORDER BY updatedAt DESC")
    suspend fun getBookmarked(): List<LocalNovelProgress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: LocalNovelProgress)

    @Query("UPDATE local_novel_progress SET lastChapter = :chapter, lastPosition = :position, updatedAt = :now WHERE novelId = :novelId")
    suspend fun updatePosition(novelId: Long, chapter: Int, position: Int, now: Long)

    @Query("UPDATE local_novel_progress SET completed = 1, updatedAt = :now WHERE novelId = :novelId")
    suspend fun markCompleted(novelId: Long, now: Long)

    @Query("UPDATE local_novel_progress SET bookmarked = :bookmarked, updatedAt = :now WHERE novelId = :novelId")
    suspend fun setBookmarked(novelId: Long, bookmarked: Boolean, now: Long)

    @Query("DELETE FROM local_novel_progress WHERE novelId = :novelId")
    suspend fun deleteByNovelId(novelId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM local_novel_progress WHERE novelId = :novelId)")
    suspend fun hasProgress(novelId: Long): Boolean
}
