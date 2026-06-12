package com.curio.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey val id: Long = System.currentTimeMillis(),
    val title: String = "",
    val content: String = "",
    val contentFormatJson: String? = null,  // JSON array of FormatRange objects: [{"s":0,"e":5,"b":true,"i":false,"c":"#D4A373"}]
    val isBookmarked: Boolean = false,
    val entryType: String = "free_write", // free_write, gratitude, task_list, reflection
    val mood: String? = null,             // happy, calm, neutral, sad, anxious, excited
    val tasksJson: String? = null,        // JSON array of {text, done, order}
    val isDraft: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis()
)

enum class EntryType(val key: String, val displayName: String, val icon: String) {
    FREE_WRITE("free_write", "Free Write", "✍️"),
    GRATITUDE("gratitude", "Gratitude", "🙏"),
    TASK_LIST("task_list", "Task List", "✅"),
    REFLECTION("reflection", "Reflection", "🤔")
}
