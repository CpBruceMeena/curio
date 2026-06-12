package com.curio.app.data.repository

import android.content.Context
import com.curio.app.data.local.JournalDao
import com.curio.app.data.local.JournalDatabase
import com.curio.app.data.local.JournalEntry
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class JournalRepository(context: Context) {

    private val dao: JournalDao = JournalDatabase.getInstance(context).journalDao()

    /** All non-draft entries, newest first. */
    val allEntries: Flow<List<JournalEntry>> = dao.getAllEntries()

    /** Get a single entry by ID. */
    suspend fun getEntry(id: Long): JournalEntry? = dao.getEntryById(id)

    /** Entries for a specific day. */
    fun getEntriesForDay(dayStart: Long, dayEnd: Long): Flow<List<JournalEntry>> =
        dao.getEntriesForDay(dayStart, dayEnd)

    /** Day buckets (epoch day numbers) that have entries. */
    fun getDaysWithEntries(): Flow<List<Long>> = dao.getDaysWithEntries()

    /** Save (insert or update) an entry. Returns the entry ID. */
    suspend fun saveEntry(entry: JournalEntry): Long = dao.upsertEntry(entry)

    /** Delete an entry. */
    suspend fun deleteEntry(id: Long) = dao.deleteEntryById(id)

    /** Get the latest auto-saved draft, if any. */
    suspend fun getLatestDraft(): JournalEntry? = dao.getLatestDraft()

    /** Search entries by title or content. */
    fun searchEntries(query: String): Flow<List<JournalEntry>> =
        dao.searchEntries(query)

    /** All bookmarked entries across all dates, newest first. */
    fun getBookmarkedEntries(): Flow<List<JournalEntry>> =
        dao.getBookmarkedEntries()

    // ── Stats ──

    /** Total number of non-draft entries. */
    fun getTotalEntryCount(): Flow<Int> = dao.getTotalEntryCount()

    /** Number of entries in a given month. */
    fun getEntryCountForMonth(monthStart: Long, monthEnd: Long): Flow<Int> =
        dao.getEntryCountForMonth(monthStart, monthEnd)

    /** All days (epoch day numbers) that have entries. */
    fun getEntryDays(): Flow<List<Long>> = dao.getEntryDays()

    /**
     * Calculate current writing streak (consecutive days with at least one entry,
     * counting backwards from today or yesterday).
     */
    fun calculateStreak(entryDays: List<Long>): Int {
        if (entryDays.isEmpty()) return 0
        val today = System.currentTimeMillis() / 86400000
        val daysSet = entryDays.toSet()

        var streak = 0
        var checkDay = today

        // If no entry today, check from yesterday
        if (today !in daysSet) {
            checkDay = today - 1
        }

        while (checkDay in daysSet) {
            streak++
            checkDay--
        }

        return streak
    }

    /** Helper: get start/end of a day in epoch millis for a given date. */
    companion object {
        fun getDayBoundaries(year: Int, month: Int, day: Int): Pair<Long, Long> {
            val cal = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val end = cal.timeInMillis
            return Pair(start, end)
        }

        fun getMonthBoundaries(year: Int, month: Int): Pair<Long, Long> {
            val cal = Calendar.getInstance().apply {
                set(year, month, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val end = cal.timeInMillis
            return Pair(start, end)
        }
    }
}
