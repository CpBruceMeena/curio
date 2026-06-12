package com.curio.app.data.model

import com.google.gson.annotations.SerializedName

// ── Novels ─────────────────────────────────────────────────────

data class Novel(
    val id: Long = 0,
    val title: String = "",
    val author: String = "",
    @SerializedName("cover_image_url")
    val coverImageUrl: String = "",
    val description: String = "",
    val source: String = "gutenberg",
    @SerializedName("source_url")
    val sourceUrl: String = "",
    @SerializedName("total_chapters")
    val totalChapters: Int = 0,
    val language: String = "en",
    @SerializedName("category_id")
    val categoryId: Long = 0,
    val likes: Int = 0,
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class NovelListResponse(
    val novels: List<Novel> = emptyList(),
    val total: Long = 0,
    val page: Int = 1,
    val limit: Int = 20
)

data class NovelChapter(
    val id: Long = 0,
    @SerializedName("novel_id")
    val novelId: Long = 0,
    @SerializedName("chapter_number")
    val chapterNumber: Int = 0,
    val title: String = "",
    val body: String = "",
    @SerializedName("read_time_secs")
    val readTimeSecs: Int = 0,
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class NovelChaptersResponse(
    val chapters: List<NovelChapter> = emptyList(),
    val total: Int = 0
)

data class NovelProgress(
    val id: Long = 0,
    @SerializedName("device_id")
    val deviceId: String = "",
    @SerializedName("novel_id")
    val novelId: Long = 0,
    @SerializedName("last_chapter")
    val lastChapter: Int = 1,
    @SerializedName("last_position")
    val lastPosition: Int = 0,
    val completed: Boolean = false,
    val bookmarked: Boolean = false,
    @SerializedName("updated_at")
    val updatedAt: String = ""
)

data class NovelProgressRequest(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("last_chapter")
    val lastChapter: Int? = null,
    @SerializedName("last_position")
    val lastPosition: Int? = null,
    val completed: Boolean? = null,
    val bookmarked: Boolean? = null
)

data class NovelDetailResponse(
    val novel: Novel = Novel(),
    val chapters: List<NovelChapter> = emptyList(),
    val progress: NovelProgress? = null
)

data class LikeResponse(
    val likes: Int = 0
)
