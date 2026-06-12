package com.curio.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedContentDao {

    @Query("SELECT * FROM cached_content ORDER BY cachedAt ASC")
    suspend fun getAll(): List<CachedContent>

    @Query("SELECT * FROM cached_content WHERE id = :contentId")
    suspend fun getById(contentId: Long): CachedContent?

    @Query("SELECT * FROM cached_content WHERE l1Name = :l1Name ORDER BY RANDOM()")
    suspend fun getByL1(l1Name: String): List<CachedContent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedContent>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CachedContent)

    @Query("DELETE FROM cached_content WHERE id = :contentId")
    suspend fun deleteById(contentId: Long)

    @Query("SELECT COUNT(*) FROM cached_content")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM cached_content WHERE l1Name = :l1Name")
    suspend fun countByL1(l1Name: String): Int

    @Query("DELETE FROM cached_content")
    suspend fun clearAll()
}
