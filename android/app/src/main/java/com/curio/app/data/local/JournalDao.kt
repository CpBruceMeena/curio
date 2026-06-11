package com.curio.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Query("SELECT * FROM journal_entries WHERE isDraft = 0 ORDER BY dateCreated DESC")
    fun getAllEntries(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE id = :entryId")
    suspend fun getEntryById(entryId: Long): JournalEntry?

    @Query("SELECT * FROM journal_entries WHERE isDraft = 0 AND dateCreated >= :dayStart AND dateCreated < :dayEnd ORDER BY dateCreated DESC")
    fun getEntriesForDay(dayStart: Long, dayEnd: Long): Flow<List<JournalEntry>>

    @Query("SELECT DISTINCT dateCreated / 86400000 AS day FROM journal_entries WHERE isDraft = 0")
    fun getDaysWithEntries(): Flow<List<Long>>

    @Query("SELECT * FROM journal_entries WHERE isDraft = 1 ORDER BY dateModified DESC LIMIT 1")
    suspend fun getLatestDraft(): JournalEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: JournalEntry): Long

    @Update
    suspend fun updateEntry(entry: JournalEntry)

    @Delete
    suspend fun deleteEntry(entry: JournalEntry)

    @Query("DELETE FROM journal_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: Long)

    @Query("SELECT * FROM journal_entries WHERE isDraft = 0 AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY dateCreated DESC")
    fun searchEntries(query: String): Flow<List<JournalEntry>>
}
