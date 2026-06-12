package com.curio.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_novel_progress")
data class LocalNovelProgress(
    @PrimaryKey val novelId: Long = 0,
    val lastChapter: Int = 1,
    val lastPosition: Int = 0,
    val completed: Boolean = false,
    val bookmarked: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
