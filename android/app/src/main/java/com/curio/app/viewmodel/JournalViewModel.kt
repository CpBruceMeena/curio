package com.curio.app.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.curio.app.data.local.EntryType
import com.curio.app.data.local.JournalEntry
import com.curio.app.data.local.PreferencesHelper
import com.curio.app.data.repository.JournalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

// ── Rich text formatting ──

data class FormatRange(
    val s: Int,       // start index
    val e: Int,       // end index
    val b: Boolean = false,  // bold
    val i: Boolean = false,  // italic
    val c: String? = null    // color hex, e.g. "#D4A373"
)

data class JournalFontSettings(
    val fontFamily: String = "serif",       // serif, sans_serif, monospace, cursive
    val fontSize: String = "medium",         // small, medium, large, xlarge
    val lineSpacing: Float = 1.8f             // 1.2f (tight), 1.5f (normal), 1.8f (relaxed)
)

data class WritingPrompt(
    val id: String,
    val icon: String,
    val title: String,
    val description: String,
    val type: String = EntryType.FREE_WRITE.key
)

val quickStartPrompts = listOf(
    WritingPrompt("morning_pages", "☀️", "Morning Pages", "Clear your mind with free-form stream-of-consciousness writing"),
    WritingPrompt("highlights", "🌟", "Today's Highlights", "Capture what stood out most about your day"),
    WritingPrompt("gratitude", "🙏", "Daily Gratitude", "List 3 things you're grateful for today", EntryType.GRATITUDE.key),
    WritingPrompt("reflection", "🌙", "Evening Reflection", "Reflect on what went well, what could improve, and what you learned", EntryType.REFLECTION.key),
    WritingPrompt("task_list", "✅", "Task List", "Plan your day with an interactive checklist", EntryType.TASK_LIST.key),
    WritingPrompt("letter_future", "💌", "Letter to Future Self", "Write a note to your future self to read later"),
)

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val done: Boolean = false
)

data class JournalUiState(
    val entries: List<JournalEntry> = emptyList(),
    val selectedDate: Long = System.currentTimeMillis(),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val daysWithEntries: Set<Long> = emptySet(),
    val currentEntry: JournalEntry? = null,
    val isEditing: Boolean = false,
    val editTitle: String = "",
    val editContent: String = "",
    val editType: String = EntryType.FREE_WRITE.key,
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false,
    val error: String? = null,
    // Type-specific editor state
    val editTaskItems: List<TaskItem> = emptyList(),
    val editGratitudeItems: List<String> = listOf("", "", ""),
    val editGratitudePrompts: List<String> = listOf(
        "I'm grateful for...",
        "Today was special because...",
        "Someone who made a difference..."
    ),
    val editReflectionAnswers: List<String> = listOf("", "", ""),
    val editReflectionPrompts: List<String> = listOf(
        "What went well today?",
        "What could have been better?",
        "What did I learn?"
    ),
    // Formatting
    val isBoldActive: Boolean = false,
    val isItalicActive: Boolean = false,
    val activeTextColor: String? = null,
    val editFormatRanges: List<FormatRange> = emptyList(),
    // Cross-day bookmarks
    val bookmarkedEntries: List<JournalEntry> = emptyList(),
    // Stats
    val writingStreak: Int = 0,
    val totalEntries: Int = 0,
    val thisMonthEntries: Int = 0
)

class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JournalRepository(application)
    private val prefs = PreferencesHelper.getInstance(application)

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    var selectedTab by mutableStateOf(0) // 0 = list, 1 = editor, 2 = detail
    var showCalendar by mutableStateOf(false)
    private var initialLoadDone = false

    // ── Font settings ──
    var fontFamily by mutableStateOf(prefs.journalFontFamily)
    var fontSize by mutableStateOf(prefs.journalFontSize)
    var lineSpacing by mutableStateOf(prefs.journalLineSpacing)
    var showFontSettings by mutableStateOf(false)
    var showBookmarkedOnly by mutableStateOf(false)

    fun toggleBookmarkedFilter() {
        showBookmarkedOnly = !showBookmarkedOnly
        if (showBookmarkedOnly) {
            loadBookmarkedEntries()
        }
    }

    private fun loadBookmarkedEntries() {
        viewModelScope.launch {
            repository.getBookmarkedEntries().collect { bookmarked ->
                _uiState.value = _uiState.value.copy(bookmarkedEntries = bookmarked)
            }
        }
    }

    fun toggleFontSettings() {
        showFontSettings = !showFontSettings
    }

    fun updateFontFamily(family: String) {
        fontFamily = family
        prefs.journalFontFamily = family
    }

    fun updateFontSize(size: String) {
        fontSize = size
        prefs.journalFontSize = size
    }

    fun updateLineSpacing(spacing: Float) {
        lineSpacing = spacing
        prefs.journalLineSpacing = spacing
    }

    // ── Formatting toggles ──

    fun toggleBold() {
        _uiState.value = _uiState.value.copy(isBoldActive = !_uiState.value.isBoldActive)
    }

    fun toggleItalic() {
        _uiState.value = _uiState.value.copy(isItalicActive = !_uiState.value.isItalicActive)
    }

    fun setActiveColor(color: String?) {
        _uiState.value = _uiState.value.copy(activeTextColor = if (_uiState.value.activeTextColor == color) null else color)
    }

    fun applyFormatToSelection(selectionStart: Int, selectionEnd: Int) {
        if (selectionStart == selectionEnd) return
        val state = _uiState.value
        val start = minOf(selectionStart, selectionEnd)
        val end = maxOf(selectionStart, selectionEnd)

        val existing = state.editFormatRanges.toMutableList()
        // Remove any existing format range that overlaps with same styles
        existing.removeAll { range ->
            range.s >= start && range.e <= end &&
                range.b == state.isBoldActive && range.i == state.isItalicActive && range.c == state.activeTextColor
        }
        existing.add(FormatRange(s = start, e = end, b = state.isBoldActive, i = state.isItalicActive, c = state.activeTextColor))
        _uiState.value = state.copy(editFormatRanges = existing.sortedBy { it.s })
        scheduleAutoSave()
    }

    fun removeFormatFromSelection(selectionStart: Int, selectionEnd: Int) {
        if (selectionStart == selectionEnd) return
        val state = _uiState.value
        val start = minOf(selectionStart, selectionEnd)
        val end = maxOf(selectionStart, selectionEnd)
        val existing = state.editFormatRanges.filter { range ->
            !(range.s >= start && range.e <= end)
        }
        _uiState.value = state.copy(editFormatRanges = existing)
        scheduleAutoSave()
    }

    fun updateFormatRanges(ranges: List<FormatRange>) {
        _uiState.value = _uiState.value.copy(editFormatRanges = ranges.sortedBy { it.s })
    }

    // ── Bookmark toggle ──

    fun toggleBookmark(entryId: Long) {
        viewModelScope.launch {
            val entry = repository.getEntry(entryId)
            if (entry != null) {
                val updated = entry.copy(isBookmarked = !entry.isBookmarked)
                repository.saveEntry(updated)
                // Update currentEntry in state so the detail screen reflects the change
                val cur = _uiState.value.currentEntry
                if (cur?.id == entryId) {
                    _uiState.value = _uiState.value.copy(currentEntry = updated)
                }
            }
        }
    }

    init {
        loadEntries()
        loadStats()
    }

    fun toggleCalendar() {
        showCalendar = !showCalendar
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
        loadDaysWithEntries()
        loadThisMonthCount()
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
        loadDaysWithEntries()
        loadThisMonthCount()
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
        loadDaysWithEntries()
        loadThisMonthCount()
    }

    // ── Editor state ──

    fun startNewEntry() {
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            currentEntry = null,
            editTitle = "",
            editContent = "",
            editType = EntryType.FREE_WRITE.key,
            editTaskItems = emptyList(),
            editGratitudeItems = listOf("", "", ""),
            editReflectionAnswers = listOf("", "", ""),
            savedSuccess = false,
            error = null,
            isBoldActive = false,
            isItalicActive = false,
            activeTextColor = null,
            editFormatRanges = emptyList()
        )
        selectedTab = 1
    }

    fun startNewEntryWithType(type: String) {
        startNewEntry()
        _uiState.value = _uiState.value.copy(editType = type)
    }

    fun startNewEntryWithPrompt(prompt: WritingPrompt) {
        startNewEntry()
        val state = _uiState.value
        when (prompt.id) {
            "morning_pages" -> {
                _uiState.value = state.copy(
                    editType = EntryType.FREE_WRITE.key,
                    editTitle = "Morning Pages",
                    editContent = ""
                )
            }
            "highlights" -> {
                _uiState.value = state.copy(
                    editType = EntryType.FREE_WRITE.key,
                    editTitle = "Today's Highlights",
                    editContent = ""
                )
            }
            "letter_future" -> {
                _uiState.value = state.copy(
                    editType = EntryType.FREE_WRITE.key,
                    editTitle = "Letter to Future Self",
                    editContent = ""
                )
            }
            "gratitude" -> {
                _uiState.value = state.copy(
                    editType = EntryType.GRATITUDE.key,
                    editTitle = "Daily Gratitude",
                    editGratitudeItems = listOf("", "", "")
                )
            }
            "reflection" -> {
                _uiState.value = state.copy(
                    editType = EntryType.REFLECTION.key,
                    editTitle = "Evening Reflection",
                    editReflectionAnswers = listOf("", "", "")
                )
            }
            "task_list" -> {
                _uiState.value = state.copy(
                    editType = EntryType.TASK_LIST.key,
                    editTitle = "Today's Tasks",
                    editTaskItems = emptyList()
                )
            }
        }
    }

    fun startEditEntry(entry: JournalEntry) {
        val taskItems = if (entry.entryType == "task_list" && !entry.tasksJson.isNullOrBlank()) {
            parseTasksJson(entry.tasksJson)
        } else emptyList<TaskItem>()

        val gratitudeItems = if (entry.entryType == "gratitude") {
            parseGuidedFields(entry.content, 3)
        } else listOf("", "", "")

        val reflectionAnswers = if (entry.entryType == "reflection") {
            parseGuidedFields(entry.content, 3)
        } else listOf("", "", "")

        val formatRanges = if (!entry.contentFormatJson.isNullOrBlank()) {
            parseFormatsJson(entry.contentFormatJson)
        } else emptyList()

        _uiState.value = _uiState.value.copy(
            isEditing = true,
            currentEntry = entry,
            editTitle = entry.title,
            editContent = entry.content,
            editType = entry.entryType,
            editTaskItems = taskItems,
            editGratitudeItems = gratitudeItems,
            editReflectionAnswers = reflectionAnswers,
            savedSuccess = false,
            error = null,
            isBoldActive = false,
            isItalicActive = false,
            activeTextColor = null,
            editFormatRanges = formatRanges
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
            editType = EntryType.FREE_WRITE.key,
            editTaskItems = emptyList(),
            editGratitudeItems = listOf("", "", ""),
            editReflectionAnswers = listOf("", "", ""),
            savedSuccess = false,
            error = null,
            isBoldActive = false,
            isItalicActive = false,
            activeTextColor = null,
            editFormatRanges = emptyList()
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

    fun updateType(type: String) {
        val base = _uiState.value.copy(editType = type)
        _uiState.value = when (type) {
            "task_list" -> base.copy(editTaskItems = if (base.editTaskItems.isEmpty()) listOf(TaskItem()) else base.editTaskItems)
            "gratitude" -> base.copy(editGratitudeItems = if (base.editGratitudeItems.all { it.isBlank() }) listOf("", "", "") else base.editGratitudeItems)
            "reflection" -> base.copy(editReflectionAnswers = if (base.editReflectionAnswers.all { it.isBlank() }) listOf("", "", "") else base.editReflectionAnswers)
            else -> base
        }
    }

    // ── Type-specific editor actions ──

    fun addTaskItem() {
        val items = _uiState.value.editTaskItems + TaskItem()
        _uiState.value = _uiState.value.copy(editTaskItems = items)
        scheduleAutoSave()
    }

    fun updateTaskText(taskId: String, text: String) {
        val items = _uiState.value.editTaskItems.map {
            if (it.id == taskId) it.copy(text = text) else it
        }
        _uiState.value = _uiState.value.copy(editTaskItems = items)
        scheduleAutoSave()
    }

    fun toggleTaskDone(taskId: String) {
        val items = _uiState.value.editTaskItems.map {
            if (it.id == taskId) it.copy(done = !it.done) else it
        }
        _uiState.value = _uiState.value.copy(editTaskItems = items)
        scheduleAutoSave()
    }

    fun deleteTaskItem(taskId: String) {
        val items = _uiState.value.editTaskItems.filter { it.id != taskId }
        _uiState.value = _uiState.value.copy(editTaskItems = items)
        scheduleAutoSave()
    }

    fun updateGratitudeItem(index: Int, text: String) {
        val items = _uiState.value.editGratitudeItems.toMutableList()
        if (index in items.indices) items[index] = text
        _uiState.value = _uiState.value.copy(editGratitudeItems = items)
        scheduleAutoSave()
    }

    fun updateReflectionAnswer(index: Int, text: String) {
        val answers = _uiState.value.editReflectionAnswers.toMutableList()
        if (index in answers.indices) answers[index] = text
        _uiState.value = _uiState.value.copy(editReflectionAnswers = answers)
        scheduleAutoSave()
    }

    fun saveEntry() {
        val state = _uiState.value

        // Build content from type-specific fields
        val content = when (state.editType) {
            "gratitude" -> {
                val items = state.editGratitudeItems
                buildString {
                    items.forEachIndexed { i, text ->
                        if (text.isNotBlank()) {
                            appendLine("• ${state.editGratitudePrompts.getOrElse(i) { "" }}")
                            appendLine("  $text")
                            appendLine()
                        }
                    }
                }.trimEnd()
            }
            "reflection" -> {
                val answers = state.editReflectionAnswers
                buildString {
                    answers.forEachIndexed { i, text ->
                        if (text.isNotBlank()) {
                            appendLine(state.editReflectionPrompts.getOrElse(i) { "" })
                            appendLine(text)
                            appendLine()
                        }
                    }
                }.trimEnd()
            }
            "task_list" -> {
                state.editTaskItems.joinToString("\n") { task ->
                    if (task.done) "- [x] ${task.text}" else "- [ ] ${task.text}"
                }
            }
            else -> state.editContent.trim()
        }

        val tasksJson = if (state.editType == "task_list") {
            serializeTasksJson(state.editTaskItems)
        } else null

        if (state.editTitle.isBlank() && content.isBlank()) {
            _uiState.value = state.copy(error = "Add a title or some content")
            return
        }

        _uiState.value = state.copy(isSaving = true, error = null)

        viewModelScope.launch {
            val contentFormatJson = if (state.editFormatRanges.isNotEmpty()) {
                serializeFormatRanges(state.editFormatRanges)
            } else null

            val entry = JournalEntry(
                id = state.currentEntry?.id ?: System.currentTimeMillis(),
                title = state.editTitle.trim().ifEmpty { when (state.editType) {
                    "gratitude" -> "My Gratitudes"
                    "task_list" -> "My Tasks"
                    "reflection" -> "My Reflection"
                    else -> "Untitled"
                }},
                content = content,
                contentFormatJson = contentFormatJson,
                isBookmarked = state.currentEntry?.isBookmarked ?: false,
                entryType = state.editType,
                mood = null,
                tasksJson = tasksJson,
                isDraft = false,
                dateCreated = state.currentEntry?.dateCreated ?: System.currentTimeMillis(),
                dateModified = System.currentTimeMillis()
            )

            try {
                repository.saveEntry(entry)
                val draft = repository.getLatestDraft()
                if (draft != null) repository.deleteEntry(draft.id)

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedSuccess = true,
                    isEditing = false,
                    currentEntry = null,
                    editTitle = "",
                    editContent = "",
                    editType = EntryType.FREE_WRITE.key,
                    editTaskItems = emptyList(),
                    editGratitudeItems = listOf("", "", ""),
                    editReflectionAnswers = listOf("", "", ""),
                    isBoldActive = false,
                    isItalicActive = false,
                    activeTextColor = null,
                    editFormatRanges = emptyList()
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
                if (_uiState.value.currentEntry?.id == entryId) cancelEditing()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete")
            }
        }
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
        loadDaysWithEntries()
        loadEntriesForDay(System.currentTimeMillis(), autoOpenIfEmpty = true)
        loadThisMonthCount()
    }

    private fun loadStats() {
        viewModelScope.launch {
            repository.getTotalEntryCount().collect { count ->
                _uiState.value = _uiState.value.copy(totalEntries = count)
            }
        }
        viewModelScope.launch {
            repository.getEntryDays().collect { days ->
                val streak = repository.calculateStreak(days)
                _uiState.value = _uiState.value.copy(
                    daysWithEntries = days.toSet(),
                    writingStreak = streak
                )
            }
        }
    }

    private fun loadThisMonthCount() {
        val state = _uiState.value
        viewModelScope.launch {
            val (start, end) = JournalRepository.getMonthBoundaries(state.selectedYear, state.selectedMonth)
            repository.getEntryCountForMonth(start, end).collect { count ->
                _uiState.value = _uiState.value.copy(thisMonthEntries = count)
            }
        }
    }

    private fun loadEntriesForDay(dayMillis: Long, autoOpenIfEmpty: Boolean = false) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
            val (start, end) = JournalRepository.getDayBoundaries(
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            )
            var firstEmission = true
            repository.getEntriesForDay(start, end).collect { entries ->
                _uiState.value = _uiState.value.copy(entries = entries)
                if (firstEmission) {
                    firstEmission = false
                    initialLoadDone = true
                    if (autoOpenIfEmpty && entries.isEmpty() && selectedTab == 0) {
                        selectedTab = 3 // Show prompt selector instead of directly opening editor
                    }
                }
            }
        }
    }

    private fun loadDaysWithEntries() {
        // Stats loading handles daysWithEntries now
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(2000)
            val state = _uiState.value
            if (state.editTitle.isNotBlank() || state.editContent.isNotBlank() ||
                state.editTaskItems.any { it.text.isNotBlank() } ||
                state.editGratitudeItems.any { it.isNotBlank() } ||
                state.editReflectionAnswers.any { it.isNotBlank() }) {

                val contentForDraft = when (state.editType) {
                    "task_list" -> state.editTaskItems.joinToString("\n") { task ->
                        if (task.done) "- [x] ${task.text}" else "- [ ] ${task.text}"
                    }
                    "gratitude" -> state.editGratitudeItems.joinToString("\n")
                    "reflection" -> state.editReflectionAnswers.joinToString("\n")
                    else -> state.editContent
                }

                val draft = JournalEntry(
                    id = state.currentEntry?.id ?: -1,
                    title = state.editTitle.trim().ifEmpty { "Draft" },
                    content = contentForDraft.trim(),
                    entryType = state.editType,
                mood = null,
                isDraft = true,
                dateCreated = state.currentEntry?.dateCreated ?: System.currentTimeMillis(),
                    dateModified = System.currentTimeMillis()
                )
                repository.saveEntry(draft)
            }
        }
    }

    // ── JSON serialization helpers ──

    private fun serializeTasksJson(items: List<TaskItem>): String {
        val sb = StringBuilder("[")
        items.forEachIndexed { i, item ->
            if (i > 0) sb.append(",")
            sb.append("""{"text":"${item.text.replace("\"", "\\\"")}","done":${item.done}}""")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun parseTasksJson(json: String): List<TaskItem> {
        val items = mutableListOf<TaskItem>()
        val regex = """\{"text":"(.*?)","done":(true|false)\}""".toRegex()
        regex.findAll(json).forEach { match ->
            items.add(TaskItem(
                text = match.groupValues[1],
                done = match.groupValues[2].toBoolean()
            ))
        }
        return items
    }

    private fun serializeFormatRanges(ranges: List<FormatRange>): String {
        val sb = StringBuilder("[")
        ranges.forEachIndexed { i, r ->
            if (i > 0) sb.append(",")
            sb.append("""{"s":${r.s},"e":${r.e},"b":${r.b},"i":${r.i},"c":"${r.c ?: ""}"}""")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun parseGuidedFields(content: String, count: Int): List<String> {
        val lines = content.lines().filter { it.isNotBlank() && !it.startsWith("•") }
        val results = mutableListOf<String>()
        // Skip prompt lines, take content lines
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank() && !trimmed.endsWith("?") && !trimmed.endsWith(":")) {
                results.add(trimmed)
            }
        }
        while (results.size < count) results.add("")
        return results.take(count)
    }

    /** Parse format ranges JSON into a list of FormatRange objects. */
    private fun parseFormatsJson(json: String): List<FormatRange> {
        val ranges = mutableListOf<FormatRange>()
        try {
            val regex = """\{"s":(\d+),"e":(\d+),"b":(true|false),"i":(true|false),"c":"(.*?)"\}""".toRegex()
            regex.findAll(json).forEach { match ->
                ranges.add(
                    FormatRange(
                        s = match.groupValues[1].toInt(),
                        e = match.groupValues[2].toInt(),
                        b = match.groupValues[3].toBoolean(),
                        i = match.groupValues[4].toBoolean(),
                        c = match.groupValues[5].ifBlank { null }
                    )
                )
            }
        } catch (_: Exception) {}
        return ranges
    }
}
