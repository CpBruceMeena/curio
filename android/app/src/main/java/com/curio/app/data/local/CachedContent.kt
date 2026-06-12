package com.curio.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for offline-cached content items.
 * Populated on first launch (25 items per L1 category) so the app
 * can show content even without network access.
 */
@Entity(tableName = "cached_content")
data class CachedContent(
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
    val l1Name: String = "",
    val cachedAt: Long = System.currentTimeMillis()
)
