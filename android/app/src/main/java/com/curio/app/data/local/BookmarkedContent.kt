package com.curio.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarked_content")
data class BookmarkedContent(
    @PrimaryKey val id: Long = 0,
    val categoryId: Long = 0,
    val categoryName: String = "",
    val title: String = "",
    val body: String = "",
    val description: String = "",
    val poet: String = "",
    val source: String = "",
    val sourceUrl: String = "",
    val readTimeSecs: Int = 15,
    val tags: String = "",
    val likes: Int = 0,
    val createdAt: String = "",
    val bookmarkedAt: Long = System.currentTimeMillis()
)
