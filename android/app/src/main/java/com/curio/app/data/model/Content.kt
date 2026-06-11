package com.curio.app.data.model

import com.google.gson.annotations.SerializedName

data class Content(
    val id: Long = 0,
    @SerializedName("category_id")
    val categoryId: Long = 0,
    @SerializedName("category_name")
    val categoryName: String = "",
    val title: String = "",
    val body: String = "",
    val description: String = "",
    val poet: String = "",
    val source: String = "",
    @SerializedName("source_url")
    val sourceUrl: String = "",
    @SerializedName("read_time_secs")
    val readTimeSecs: Int = 15,
    val tags: String = "",
    val likes: Int = 0,
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class FeedResponse(
    val content: List<Content> = emptyList(),
    val page: Int = 1,
    @SerializedName("page_size")
    val pageSize: Int = 10,
    val total: Long = 0,
    @SerializedName("has_more")
    val hasMore: Boolean = false
)

data class Category(
    val id: Long = 0,
    val name: String = "",
    val icon: String = "",
    @SerializedName("color_hex")
    val colorHex: String = "",
    val priority: Int = 0,
    @SerializedName("content_count")
    val contentCount: Long = 0,
    @SerializedName("l1_category")
    val l1Category: String = ""
)

data class CategoriesResponse(
    val categories: List<Category> = emptyList(),
    val total: Int = 0
)

data class FeedbackRequest(
    val message: String,
    @SerializedName("device_id")
    val deviceId: String = ""
)

data class FeedbackResponse(
    val success: Boolean = false,
    val message: String = "",
    val id: Long = 0
)

data class L1Group(
    val name: String = "",
    val icon: String = "",
    @SerializedName("color_hex")
    val colorHex: String = "",
    val categories: List<Category> = emptyList()
)

data class L1CategoriesResponse(
    val groups: List<L1Group> = emptyList(),
    val total: Int = 0
)

data class Puzzle(
    val id: Long = 0,
    @SerializedName("puzzle_type")
    val puzzleType: String = "",
    @SerializedName("category_id")
    val categoryId: Long = 0,
    val title: String = "",
    val question: String = "",
    @SerializedName("answer_type")
    val answerType: String = "text",
    val options: String? = null,
    val hint: String = "",
    val explanation: String = "",
    val difficulty: Int = 1,
    val likes: Int = 0
)

data class PuzzleResponse(
    val puzzles: List<Puzzle> = emptyList(),
    val total: Long = 0
)

data class ValidateResponse(
    val correct: Boolean = false,
    val explanation: String = ""
)

data class ValidateRequest(
    val answer: String
)

// ── Device Info ────────────────────────────────────────────────

data class DeviceInfo(
    val id: Long = 0,
    @SerializedName("device_id")
    val deviceId: String = "",
    @SerializedName("os_version")
    val osVersion: String = "",
    @SerializedName("app_version")
    val appVersion: String = "",
    @SerializedName("device_model")
    val deviceModel: String = "",
    val manufacturer: String = "",
    @SerializedName("screen_size")
    val screenSize: String = "",
    val language: String = "",
    val timezone: String = "",
    @SerializedName("created_at")
    val createdAt: String = "",
    @SerializedName("updated_at")
    val updatedAt: String = ""
)

data class DeviceInfoRequest(
    @SerializedName("os_version")
    val osVersion: String,
    @SerializedName("app_version")
    val appVersion: String,
    @SerializedName("device_model")
    val deviceModel: String,
    val manufacturer: String,
    @SerializedName("screen_size")
    val screenSize: String,
    val language: String,
    val timezone: String
)

// ── Comments ───────────────────────────────────────────────────

data class CommentEntry(
    val id: String = "",
    val text: String = "",
    @SerializedName("device_id")
    val deviceId: String = "",
    val email: String = "",
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class CommentsResponse(
    @SerializedName("content_id")
    val contentId: Long = 0,
    val comments: List<CommentEntry> = emptyList(),
    val total: Int = 0
)

data class AddCommentRequest(
    val text: String,
    val email: String = ""
)

data class AddCommentResponse(
    val success: Boolean = false,
    val comment: CommentEntry? = null
)

// ── TTS (Text-to-Speech) ───────────────────────────────────────────
data class TtsRequest(
    @SerializedName("content_id")
    val contentId: Long? = null,
    val text: String? = null,
    val voice: String = "en-US-JennyNeural"
)
