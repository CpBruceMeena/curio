package com.curio.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface OfflineNovelDao {

    @Query("SELECT * FROM offline_novels ORDER BY downloadedAt DESC")
    suspend fun getAll(): List<OfflineNovel>

    @Query("SELECT * FROM offline_novels WHERE id = :novelId")
    suspend fun getById(novelId: Long): OfflineNovel?

    @Query("SELECT * FROM offline_novels WHERE downloadCompleted = 0 ORDER BY downloadedAt DESC")
    suspend fun getPendingDownloads(): List<OfflineNovel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(novel: OfflineNovel)

    @Update
    suspend fun update(novel: OfflineNovel)

    @Query("UPDATE offline_novels SET chaptersDownloaded = :count WHERE id = :novelId")
    suspend fun updateDownloadProgress(novelId: Long, count: Int)

    @Query("UPDATE offline_novels SET downloadCompleted = 1 WHERE id = :novelId")
    suspend fun markDownloadComplete(novelId: Long)

    @Query("DELETE FROM offline_novels WHERE id = :novelId")
    suspend fun deleteById(novelId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM offline_novels WHERE id = :novelId)")
    suspend fun isDownloaded(novelId: Long): Boolean

    @Query("SELECT COUNT(*) FROM offline_novels")
    suspend fun count(): Int
}
