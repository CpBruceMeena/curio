package com.curio.app.data.api

import com.curio.app.data.model.CategoriesResponse
import com.curio.app.data.model.Content
import com.curio.app.data.model.FeedResponse
import retrofit2.Response
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
        @Path("id") contentId: Long
    ): Response<Map<String, Int>>

    @GET("categories")
    suspend fun getCategories(): Response<CategoriesResponse>
}
