package com.curio.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BookmarkedContentDao {

    @Query("SELECT * FROM bookmarked_content ORDER BY bookmarkedAt DESC")
    suspend fun getAll(): List<BookmarkedContent>

    @Query("SELECT * FROM bookmarked_content WHERE id = :contentId")
    suspend fun getById(contentId: Long): BookmarkedContent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(content: BookmarkedContent)

    @Query("DELETE FROM bookmarked_content WHERE id = :contentId")
    suspend fun deleteById(contentId: Long)

    @Query("SELECT COUNT(*) FROM bookmarked_content")
    suspend fun count(): Int
}
