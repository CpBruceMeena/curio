package com.curio.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_novels")
data class OfflineNovel(
    @PrimaryKey val id: Long = 0,
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val source: String = "gutenberg",
    val sourceUrl: String = "",
    val totalChapters: Int = 0,
    val language: String = "en",
    val coverGradientIndex: Int = 0,
    val chaptersDownloaded: Int = 0,
    val downloadCompleted: Boolean = false,
    val downloadedAt: Long = System.currentTimeMillis()
)
