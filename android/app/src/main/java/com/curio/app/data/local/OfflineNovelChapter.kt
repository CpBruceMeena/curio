package com.curio.app.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "offline_novel_chapters",
    primaryKeys = ["novelId", "chapterNumber"],
    indices = [Index("novelId")]
)
data class OfflineNovelChapter(
    val novelId: Long = 0,
    val chapterNumber: Int = 0,
    val title: String = "",
    val body: String = "",
    val readTimeSecs: Int = 0
)
