package com.curio.app.data.repository

import com.curio.app.data.api.RetrofitClient
import com.curio.app.data.model.CategoriesResponse
import com.curio.app.data.model.Content
import com.curio.app.data.model.FeedbackRequest
import com.curio.app.data.model.FeedbackResponse
import com.curio.app.data.model.FeedResponse
import com.curio.app.data.model.L1CategoriesResponse
import com.curio.app.data.model.PuzzleResponse
import com.curio.app.data.model.ValidateRequest
import com.curio.app.data.model.ValidateResponse

class ContentRepository {

    private val api = RetrofitClient.api

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

    suspend fun likeContent(contentId: Long): Result<Int> {
        return try {
            val response = api.likeContent(contentId)
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

    suspend fun submitFeedback(message: String): Result<FeedbackResponse> {
        return try {
            val response = api.submitFeedback(FeedbackRequest(message = message))
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
}
