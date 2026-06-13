package com.curio.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_annotations")
data class SavedAnnotation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val novelId: Long = 0,
    val chapterNumber: Int = 0,
    val selectedText: String = "",
    val note: String = "",
    val startPosition: Int = 0,
    val endPosition: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
