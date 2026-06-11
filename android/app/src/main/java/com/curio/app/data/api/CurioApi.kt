package com.curio.app.data.api

import com.curio.app.data.model.AddCommentRequest
import com.curio.app.data.model.AddCommentResponse
import com.curio.app.data.model.CategoriesResponse
import com.curio.app.data.model.CommentsResponse
import com.curio.app.data.model.Content
import com.curio.app.data.model.FeedbackRequest
import com.curio.app.data.model.FeedbackResponse
import com.curio.app.data.model.FeedResponse
import com.curio.app.data.model.L1CategoriesResponse
import com.curio.app.data.model.PuzzleResponse
import com.curio.app.data.model.ValidateRequest
import com.curio.app.data.model.ValidateResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CurioApi {

    @GET("feed")
    suspend fun getFeed(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 100,
        @Query("category_id") categoryId: Long? = null,
        @Query("random") random: Boolean = false
    ): Response<FeedResponse>

    @GET("content/{id}")
    suspend fun getContent(
        @Path("id") contentId: Long
    ): Response<Content>

    @POST("content/{id}/like")
    suspend fun likeContent(
        @Path("id") contentId: Long,
        @Query("action") action: String = "like"
    ): Response<Map<String, Int>>

    @GET("categories")
    suspend fun getCategories(): Response<CategoriesResponse>

    @GET("categories/l1")
    suspend fun getL1Categories(): Response<L1CategoriesResponse>

    @POST("feedback")
    suspend fun submitFeedback(
        @Body request: FeedbackRequest
    ): Response<FeedbackResponse>

    // ── Comments ───────────────────────────────────────────────
    @GET("content/{id}/comments")
    suspend fun getComments(
        @Path("id") contentId: Long
    ): Response<CommentsResponse>

    @POST("content/{id}/comments")
    suspend fun addComment(
        @Path("id") contentId: Long,
        @Body request: AddCommentRequest,
        @Query("device_id") deviceId: String = ""
    ): Response<AddCommentResponse>

    // ── Puzzles ────────────────────────────────────────────────
    @GET("puzzles")
    suspend fun getPuzzles(
        @Query("type") puzzleType: String? = null,
        @Query("category_id") categoryId: Long? = null,
        @Query("limit") limit: Int = 20
    ): Response<PuzzleResponse>

    @POST("puzzles/{id}/validate")
    suspend fun validatePuzzle(
        @Path("id") puzzleId: Long,
        @Body request: ValidateRequest
    ): Response<ValidateResponse>

    @POST("puzzles/{id}/like")
    suspend fun likePuzzle(
        @Path("id") puzzleId: Long
    ): Response<Map<String, Int>>
}
