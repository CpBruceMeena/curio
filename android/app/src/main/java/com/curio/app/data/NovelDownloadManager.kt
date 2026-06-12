package com.curio.app.data

import android.content.Context
import com.curio.app.data.api.RetrofitClient
import com.curio.app.data.local.JournalDatabase
import com.curio.app.data.local.LocalNovelProgress
import com.curio.app.data.local.OfflineNovel
import com.curio.app.data.local.OfflineNovelChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class DownloadProgress(
    val novelId: Long,
    val totalChapters: Int = 0,
    val chaptersDownloaded: Int = 0,
    val isReadyToRead: Boolean = false,  // true after chapter 1 body is cached
    val isComplete: Boolean = false,
    val error: String? = null
)

class NovelDownloadManager(private val context: Context) {

    private val api = RetrofitClient.api
    private val db = JournalDatabase.getInstance(context)

    private val _downloadProgress = MutableStateFlow<Map<Long, DownloadProgress>>(emptyMap())
    val downloadProgress: StateFlow<Map<Long, DownloadProgress>> = _downloadProgress.asStateFlow()

    /** Check if a novel is already fully downloaded. */
    suspend fun isDownloaded(novelId: Long): Boolean = db.offlineNovelDao().isDownloaded(novelId)

    /** Check if at least chapter 1 is available (ready to start reading). */
    suspend fun isReadyToRead(novelId: Long): Boolean {
        val ch1 = db.offlineNovelChapterDao().getChapter(novelId, 1)
        return ch1 != null && ch1.body.isNotBlank()
    }

    /**
     * Download a novel progressively:
     *   1. Fetch novel metadata + chapter list (fast)
     *   2. Save metadata to Room
     *   3. Fetch chapter 1 body → emit isReadyToRead=true
     *   4. Fetch remaining chapters in background, emitting progress
     */
    suspend fun downloadNovel(novelId: Long) = withContext(Dispatchers.IO) {
        // Step 1: Fetch metadata + chapter list from API
        val api = RetrofitClient.api
        val detailResponse = api.getNovel(novelId)
        if (!detailResponse.isSuccessful || detailResponse.body() == null) {
            _downloadProgress.value = _downloadProgress.value + (novelId to DownloadProgress(
                novelId = novelId, error = "Failed to fetch novel info"
            ))
            return@withContext
        }
        val detail = detailResponse.body()!!
        val novel = detail.novel
        val chapters = detail.chapters
        val total = chapters.size

        // Step 2: Save novel metadata to Room
        db.offlineNovelDao().insert(OfflineNovel(
            id = novel.id,
            title = novel.title,
            author = novel.author,
            description = novel.description,
            source = novel.source,
            sourceUrl = novel.sourceUrl,
            totalChapters = total,
            language = novel.language,
            coverGradientIndex = (novel.id % 10).toInt()
        ))

        // Step 3: Fetch and save chapter 1 body first (enable reading ASAP)
        val ch1Meta = chapters.firstOrNull()
        if (ch1Meta != null) {
            val ch1Body = api.getNovelChapter(novelId, 1)
            if (ch1Body.isSuccessful && ch1Body.body() != null) {
                val ch1 = ch1Body.body()!!
                db.offlineNovelChapterDao().insert(OfflineNovelChapter(
                    novelId = novelId,
                    chapterNumber = 1,
                    title = ch1.title,
                    body = ch1.body,
                    readTimeSecs = ch1.readTimeSecs
                ))
                db.offlineNovelDao().updateDownloadProgress(novelId, 1)
            }
        }

        // Mark as ready to read (chapter 1 body cached)
        _downloadProgress.value = _downloadProgress.value + (novelId to DownloadProgress(
            novelId = novelId,
            totalChapters = total,
            chaptersDownloaded = 1,
            isReadyToRead = true
        ))

        // Step 4: Fetch remaining chapters in background (2..total)
        for (num in 2..total) {
            val chapterResponse = api.getNovelChapter(novelId, num)
            if (chapterResponse.isSuccessful && chapterResponse.body() != null) {
                val ch = chapterResponse.body()!!
                db.offlineNovelChapterDao().insert(OfflineNovelChapter(
                    novelId = novelId,
                    chapterNumber = num,
                    title = ch.title,
                    body = ch.body,
                    readTimeSecs = ch.readTimeSecs
                ))
                db.offlineNovelDao().updateDownloadProgress(novelId, num)
            }

            _downloadProgress.value = _downloadProgress.value + (novelId to DownloadProgress(
                novelId = novelId,
                totalChapters = total,
                chaptersDownloaded = num,
                isReadyToRead = true,
                isComplete = num == total
            ))
        }

        // Mark download complete
        db.offlineNovelDao().markDownloadComplete(novelId)

        // Initialize empty progress for this novel
        if (db.localNovelProgressDao().getProgress(novelId) == null) {
            db.localNovelProgressDao().upsert(LocalNovelProgress(novelId = novelId))
        }
    }

    /** Delete a downloaded novel and all its chapters from local storage. */
    suspend fun deleteNovel(novelId: Long) = withContext(Dispatchers.IO) {
        db.offlineNovelChapterDao().deleteByNovelId(novelId)
        db.localNovelProgressDao().deleteByNovelId(novelId)
        db.offlineNovelDao().deleteById(novelId)
        _downloadProgress.value = _downloadProgress.value - novelId
    }

    /** Get all downloaded novels from local storage. */
    suspend fun getDownloadedNovels(): List<OfflineNovel> = withContext(Dispatchers.IO) {
        db.offlineNovelDao().getAll()
    }
}
