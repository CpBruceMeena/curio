package com.curio.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SavedAnnotationDao {

    @Query("SELECT * FROM saved_annotations WHERE novelId = :novelId AND chapterNumber = :chapter ORDER BY startPosition ASC")
    suspend fun getByChapter(novelId: Long, chapter: Int): List<SavedAnnotation>

    @Query("SELECT * FROM saved_annotations WHERE novelId = :novelId ORDER BY chapterNumber ASC, startPosition ASC")
    suspend fun getByNovel(novelId: Long): List<SavedAnnotation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(annotation: SavedAnnotation): Long

    @Delete
    suspend fun delete(annotation: SavedAnnotation)

    @Query("DELETE FROM saved_annotations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM saved_annotations WHERE novelId = :novelId")
    suspend fun deleteByNovel(novelId: Long)
}
