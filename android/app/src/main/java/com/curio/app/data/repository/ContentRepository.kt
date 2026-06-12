package com.curio.app.data.repository

import com.curio.app.data.api.RetrofitClient
import com.curio.app.data.model.AddCommentRequest
import com.curio.app.data.model.AddCommentResponse
import com.curio.app.data.model.CategoriesResponse
import com.curio.app.data.model.CommentsResponse
import com.curio.app.data.model.Content
import com.curio.app.data.local.CachedContent
import com.curio.app.data.model.FeedbackRequest
import com.curio.app.data.model.FeedbackResponse
import com.curio.app.data.model.FeedResponse
import com.curio.app.data.model.L1CategoriesResponse
import com.curio.app.data.model.PuzzleResponse
import com.curio.app.data.model.TtsRequest
import com.curio.app.data.model.ValidateRequest
import com.curio.app.data.model.ValidateResponse

class ContentRepository(
    private val context: android.content.Context? = null
) {

    private val api = RetrofitClient.api

    private val bookmarkDao: com.curio.app.data.local.BookmarkedContentDao? by lazy {
        context?.let {
            com.curio.app.data.local.JournalDatabase.getInstance(it).bookmarkedContentDao()
        }
    }

    private val cacheDao: com.curio.app.data.local.CachedContentDao? by lazy {
        context?.let {
            com.curio.app.data.local.JournalDatabase.getInstance(it).cachedContentDao()
        }
    }

    suspend fun getFeed(
        page: Int = 1,
        pageSize: Int = 100,
        categoryId: Long? = null,
        random: Boolean = false
    ): Result<FeedResponse> {
        return try {
            val response = api.getFeed(page, pageSize, categoryId, random)
            if (response.isSuccessful) {
                Result.success(response.body() ?: FeedResponse())
            } else {
                Result.failure(Exception("Failed to fetch feed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getContent(contentId: Long): Result<Content> {
        return try {
            val response = api.getContent(contentId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Content not found: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likeContent(contentId: Long, action: String = "like"): Result<Int> {
        return try {
            val response = api.likeContent(contentId, action)
            if (response.isSuccessful) {
                Result.success(response.body()?.get("likes") ?: 0)
            } else {
                Result.failure(Exception("Failed to like: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCategories(): Result<CategoriesResponse> {
        return try {
            val response = api.getCategories()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch categories: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitFeedback(message: String, deviceId: String = ""): Result<FeedbackResponse> {
        return try {
            val response = api.submitFeedback(FeedbackRequest(message = message, deviceId = deviceId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to submit feedback: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getL1Categories(): Result<L1CategoriesResponse> {
        return try {
            val response = api.getL1Categories()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch L1 categories: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Comments ───────────────────────────────────────────────
    suspend fun getComments(contentId: Long): Result<CommentsResponse> {
        return try {
            val response = api.getComments(contentId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch comments: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addComment(contentId: Long, text: String, email: String = "", deviceId: String = ""): Result<AddCommentResponse> {
        return try {
            val response = api.addComment(contentId, AddCommentRequest(text = text, email = email), deviceId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to add comment: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Puzzles ────────────────────────────────────────────────
    suspend fun getPuzzles(
        puzzleType: String? = null,
        categoryId: Long? = null,
        limit: Int = 20
    ): Result<PuzzleResponse> {
        return try {
            val response = api.getPuzzles(puzzleType, categoryId, limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch puzzles: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validatePuzzle(puzzleId: Long, answer: String): Result<ValidateResponse> {
        return try {
            val response = api.validatePuzzle(puzzleId, ValidateRequest(answer))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to validate: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likePuzzle(puzzleId: Long): Result<Int> {
        return try {
            val response = api.likePuzzle(puzzleId)
            if (response.isSuccessful) {
                Result.success(response.body()?.get("likes") ?: 0)
            } else {
                Result.failure(Exception("Failed to like: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── TTS (Text-to-Speech) ────────────────────────────────────
    /**
     * Generate audio for a content item and save to a temp file.
     * Returns the temp file path for playback.
     */
    suspend fun generateSpeech(contentId: Long): Result<java.io.File> {
        return try {
            val responseBody = api.generateSpeech(TtsRequest(contentId = contentId))
            val file = java.io.File.createTempFile("tts_", ".mp3")
            file.outputStream().use { output ->
                responseBody.byteStream().use { input ->
                    input.copyTo(output)
                }
            }
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Local bookmark storage (offline-first) ─────────────────

    /** Save a Content item as a local bookmark (full content cached offline). */
    suspend fun saveBookmarkLocally(content: com.curio.app.data.model.Content) {
        bookmarkDao?.insert(
            com.curio.app.data.local.BookmarkedContent(
                id = content.id,
                categoryId = content.categoryId,
                categoryName = content.categoryName,
                title = content.title,
                body = content.body,
                description = content.description,
                poet = content.poet,
                source = content.source,
                sourceUrl = content.sourceUrl,
                readTimeSecs = content.readTimeSecs,
                tags = content.tags,
                likes = content.likes,
                createdAt = content.createdAt
            )
        )
    }

    /** Remove a bookmarked content item from local storage. */
    suspend fun removeBookmarkLocally(contentId: Long) {
        bookmarkDao?.deleteById(contentId)
    }

    /** Load all locally-stored bookmarked content items (fully offline). */
    suspend fun getBookmarkedContentLocally(): List<com.curio.app.data.model.Content> {
        return bookmarkDao?.getAll()?.map { entity ->
            com.curio.app.data.model.Content(
                id = entity.id,
                categoryId = entity.categoryId,
                categoryName = entity.categoryName,
                title = entity.title,
                body = entity.body,
                description = entity.description,
                poet = entity.poet,
                source = entity.source,
                sourceUrl = entity.sourceUrl,
                readTimeSecs = entity.readTimeSecs,
                tags = entity.tags,
                likes = entity.likes,
                createdAt = entity.createdAt
            )
        } ?: emptyList()
    }

    /** Check if a content ID exists in local bookmarks. */
    suspend fun isBookmarkedLocally(contentId: Long): Boolean {
        return bookmarkDao?.getById(contentId) != null
    }

    // ── Offline content cache (25 items per category) ───────────────

    /**
     * Save a list of Content items to the local cache.
     * Only saves items for L1 categories (Facts, Poems, Short Stories, Puzzles).
     */
    suspend fun saveToOfflineCache(items: List<Content>, l1Name: String) {
        val entities = items.map { item ->
            CachedContent(
                id = item.id,
                categoryId = item.categoryId,
                categoryName = item.categoryName,
                title = item.title,
                body = item.body,
                description = item.description,
                poet = item.poet,
                source = item.source,
                sourceUrl = item.sourceUrl,
                readTimeSecs = item.readTimeSecs,
                tags = item.tags,
                likes = item.likes,
                createdAt = item.createdAt,
                l1Name = l1Name
            )
        }
        cacheDao?.insertAll(entities)
    }

    /** Load all cached content for offline use. */
    suspend fun loadOfflineCache(): List<Content> {
        return cacheDao?.getAll()?.map { entity ->
            Content(
                id = entity.id,
                categoryId = entity.categoryId,
                categoryName = entity.categoryName,
                title = entity.title,
                body = entity.body,
                description = entity.description,
                poet = entity.poet,
                source = entity.source,
                sourceUrl = entity.sourceUrl,
                readTimeSecs = entity.readTimeSecs,
                tags = entity.tags,
                likes = entity.likes,
                createdAt = entity.createdAt
            )
        } ?: emptyList()
    }

    /** Check if the offline cache has been populated. */
    suspend fun isCachePopulated(): Boolean {
        return (cacheDao?.count() ?: 0) > 0
    }

    /** Get cached content for a specific L1 category. */
    suspend fun getCachedByL1(l1Name: String): List<Content> {
        return cacheDao?.getByL1(l1Name)?.map { entity ->
            Content(
                id = entity.id,
                categoryId = entity.categoryId,
                categoryName = entity.categoryName,
                title = entity.title,
                body = entity.body,
                description = entity.description,
                poet = entity.poet,
                source = entity.source,
                sourceUrl = entity.sourceUrl,
                readTimeSecs = entity.readTimeSecs,
                tags = entity.tags,
                likes = entity.likes,
                createdAt = entity.createdAt
            )
        } ?: emptyList()
    }
}
