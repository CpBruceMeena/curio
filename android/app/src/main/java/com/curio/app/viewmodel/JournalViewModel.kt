package com.curio.app.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.curio.app.data.local.EntryType
import com.curio.app.data.local.JournalEntry
import com.curio.app.data.repository.JournalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class JournalUiState(
    val entries: List<JournalEntry> = emptyList(),
    val selectedDate: Long = System.currentTimeMillis(),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val daysWithEntries: Set<Long> = emptySet(), // epoch day numbers
    val currentEntry: JournalEntry? = null,
    val isEditing: Boolean = false,
    val editTitle: String = "",
    val editContent: String = "",
    val editMood: String? = null,
    val editType: String = EntryType.FREE_WRITE.key,
    val editTags: String = "",
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false,
    val error: String? = null
)

class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JournalRepository(application)

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    var selectedTab by mutableStateOf(0) // 0 = entries list, 1 = editor

    init {
        loadEntries()
    }

    // ── Calendar navigation ──

    fun previousMonth() {
        val state = _uiState.value
        val cal = Calendar.getInstance().apply {
            set(state.selectedYear, state.selectedMonth, 1)
            add(Calendar.MONTH, -1)
        }
        _uiState.value = state.copy(
            selectedYear = cal.get(Calendar.YEAR),
            selectedMonth = cal.get(Calendar.MONTH)
        )
        loadDaysWithEntries(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun nextMonth() {
        val state = _uiState.value
        val cal = Calendar.getInstance().apply {
            set(state.selectedYear, state.selectedMonth, 1)
            add(Calendar.MONTH, 1)
        }
        _uiState.value = state.copy(
            selectedYear = cal.get(Calendar.YEAR),
            selectedMonth = cal.get(Calendar.MONTH)
        )
        loadDaysWithEntries(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun selectDate(dayOfMonth: Int) {
        val state = _uiState.value
        val cal = Calendar.getInstance().apply {
            set(state.selectedYear, state.selectedMonth, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        _uiState.value = state.copy(selectedDate = cal.timeInMillis)
        loadEntriesForDay(cal.timeInMillis)
    }

    fun selectToday() {
        val now = Calendar.getInstance()
        _uiState.value = _uiState.value.copy(
            selectedYear = now.get(Calendar.YEAR),
            selectedMonth = now.get(Calendar.MONTH),
            selectedDate = now.timeInMillis
        )
        loadEntriesForDay(now.timeInMillis)
        loadDaysWithEntries(now.get(Calendar.YEAR), now.get(Calendar.MONTH))
    }

    // ── Editor state ──

    fun startNewEntry() {
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            currentEntry = null,
            editTitle = "",
            editContent = "",
            editMood = null,
            editType = EntryType.FREE_WRITE.key,
            editTags = "",
            savedSuccess = false,
            error = null
        )
        selectedTab = 1
    }

    fun startEditEntry(entry: JournalEntry) {
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            currentEntry = entry,
            editTitle = entry.title,
            editContent = entry.content,
            editMood = entry.mood,
            editType = entry.entryType,
            editTags = entry.tags,
            savedSuccess = false,
            error = null
        )
        selectedTab = 1
    }

    fun cancelEditing() {
        selectedTab = 0
        _uiState.value = _uiState.value.copy(
            isEditing = false,
            currentEntry = null,
            editTitle = "",
            editContent = "",
            editMood = null,
            editType = EntryType.FREE_WRITE.key,
            editTags = "",
            savedSuccess = false,
            error = null
        )
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(editTitle = title)
        scheduleAutoSave()
    }

    fun updateContent(content: String) {
        _uiState.value = _uiState.value.copy(editContent = content)
        scheduleAutoSave()
    }

    fun updateMood(mood: String?) {
        _uiState.value = _uiState.value.copy(editMood = mood)
    }

    fun updateType(type: String) {
        _uiState.value = _uiState.value.copy(editType = type)
    }

    fun updateTags(tags: String) {
        _uiState.value = _uiState.value.copy(editTags = tags)
    }

    fun saveEntry() {
        val state = _uiState.value
        if (state.editTitle.isBlank() && state.editContent.isBlank()) {
            _uiState.value = state.copy(error = "Add a title or some content")
            return
        }

        _uiState.value = state.copy(isSaving = true, error = null)

        viewModelScope.launch {
            val entry = JournalEntry(
                id = state.currentEntry?.id ?: System.currentTimeMillis(),
                title = state.editTitle.trim(),
                content = state.editContent.trim(),
                entryType = state.editType,
                mood = state.editMood,
                tags = state.editTags,
                tasksJson = null,
                isDraft = false,
                dateCreated = state.currentEntry?.dateCreated ?: System.currentTimeMillis(),
                dateModified = System.currentTimeMillis()
            )

            try {
                repository.saveEntry(entry)
                // Clean up any stale draft
                val draft = repository.getLatestDraft()
                if (draft != null) {
                    repository.deleteEntry(draft.id)
                }
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedSuccess = true,
                    isEditing = false,
                    currentEntry = null,
                    editTitle = "",
                    editContent = "",
                    editMood = null,
                    editTags = ""
                )
                selectedTab = 0
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save: ${e.message}"
                )
            }
        }
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteEntry(entryId)
                if (_uiState.value.currentEntry?.id == entryId) {
                    cancelEditing()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete")
            }
        }
    }

    fun selectEntry(entry: JournalEntry) {
        _uiState.value = _uiState.value.copy(currentEntry = entry)
        selectedTab = 2
    }

    fun clearCurrentEntry() {
        _uiState.value = _uiState.value.copy(currentEntry = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ── Private ──

    private fun loadEntries() {
        viewModelScope.launch {
            repository.allEntries.collect { entries ->
                _uiState.value = _uiState.value.copy(entries = entries)
            }
        }
        val now = Calendar.getInstance()
        loadDaysWithEntries(now.get(Calendar.YEAR), now.get(Calendar.MONTH))
        loadEntriesForDay(System.currentTimeMillis())
    }

    private fun loadEntriesForDay(dayMillis: Long) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
            val (start, end) = JournalRepository.getDayBoundaries(
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            )
            repository.getEntriesForDay(start, end).collect { entries ->
                _uiState.value = _uiState.value.copy(entries = entries)
            }
        }
    }

    private fun loadDaysWithEntries(year: Int, month: Int) {
        viewModelScope.launch {
            repository.getDaysWithEntries().collect { dayBuckets ->
                _uiState.value = _uiState.value.copy(daysWithEntries = dayBuckets.toSet())
            }
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(2000) // 2 seconds after last edit
            val state = _uiState.value
            if (state.editTitle.isNotBlank() || state.editContent.isNotBlank()) {
                val draft = JournalEntry(
                    id = state.currentEntry?.id ?: -1,
                    title = state.editTitle.trim(),
                    content = state.editContent.trim(),
                    entryType = state.editType,
                    mood = state.editMood,
                    tags = state.editTags,
                    isDraft = true,
                    dateCreated = state.currentEntry?.dateCreated ?: System.currentTimeMillis(),
                    dateModified = System.currentTimeMillis()
                )
                repository.saveEntry(draft)
            }
        }
    }
}
