package com.curio.app.data.repository

import com.curio.app.data.api.RetrofitClient
import com.curio.app.data.model.Novel
import com.curio.app.data.model.NovelChapter
import com.curio.app.data.model.NovelChaptersResponse
import com.curio.app.data.model.NovelDetailResponse
import com.curio.app.data.model.NovelListResponse
import com.curio.app.data.model.NovelProgress
import com.curio.app.data.model.NovelProgressRequest
import java.io.File

class NovelRepository {

    private val api = RetrofitClient.api

    suspend fun getNovels(
        page: Int = 1,
        limit: Int = 20,
        categoryId: Long? = null
    ): Result<NovelListResponse> {
        return try {
            val response = api.getNovels(page, limit, categoryId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch novels: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNovel(
        novelId: Long,
        deviceId: String = ""
    ): Result<NovelDetailResponse> {
        return try {
            val response = api.getNovel(novelId, deviceId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch novel: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNovelChapters(novelId: Long): Result<List<NovelChapter>> {
        return try {
            val response = api.getNovelChapters(novelId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.chapters)
            } else {
                Result.failure(Exception("Failed to fetch chapters: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNovelChapter(novelId: Long, chapterNum: Int): Result<NovelChapter> {
        return try {
            val response = api.getNovelChapter(novelId, chapterNum)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch chapter: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProgress(request: NovelProgressRequest, novelId: Long): Result<NovelProgress> {
        return try {
            val response = api.updateNovelProgress(novelId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update progress: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likeNovel(novelId: Long): Result<Int> {
        return try {
            val response = api.likeNovel(novelId)
            if (response.isSuccessful) {
                Result.success(response.body()?.get("likes") ?: 0)
            } else {
                Result.failure(Exception("Failed to like novel: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
