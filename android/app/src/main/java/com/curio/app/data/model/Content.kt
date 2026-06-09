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
    val message: String
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
