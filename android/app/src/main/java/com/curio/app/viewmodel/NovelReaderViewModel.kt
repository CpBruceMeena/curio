package com.curio.app.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.curio.app.CurioApp
import com.curio.app.data.NovelDownloadManager
import com.curio.app.data.local.JournalDatabase
import com.curio.app.data.local.LocalNovelProgress
import com.curio.app.data.local.OfflineNovelChapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NovelReaderUiState(
    val novelId: Long = 0,
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val totalChapters: Int = 0,
    val chaptersDownloaded: Int = 0,
    val isDownloadCompleted: Boolean = false,

    // Current reading position
    val chapters: List<OfflineNovelChapter> = emptyList(),
    val currentChapterIndex: Int = 0,
    val currentChapterBody: String = "",
    val currentChapterTitle: String = "",
    val lastChapter: Int = 1,

    // Progress
    val isBookmarked: Boolean = false,
    val isCompleted: Boolean = false,

    // Download state
    val isDownloading: Boolean = false,
    val isReadyToRead: Boolean = false,
    val downloadError: String? = null,

    // Loading
    val isLoading: Boolean = true,
    val error: String? = null,

    // TTS
    val isPlaying: Boolean = false,
    val isTtsLoading: Boolean = false,
    val audioFilePath: String? = null,

    // Reading preferences
    val fontSize: Int = 18,
    val lineSpacing: Float = 1.6f,
    val isDarkMode: Boolean = false
)

class NovelReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JournalDatabase.getInstance(application)
    private val downloadManager = NovelDownloadManager(application)
    private val prefs = (application as CurioApp).prefs

    var uiState by mutableStateOf(NovelReaderUiState())
        private set

    private var autoSaveJob: Job? = null
    private var downloadJob: Job? = null

    // Expose download progress for the detail screen
    private val _downloadProgress = MutableStateFlow<com.curio.app.data.DownloadProgress?>(null)
    val downloadProgress: StateFlow<com.curio.app.data.DownloadProgress?> = _downloadProgress.asStateFlow()

    // ── Initialize / Load ───────────────────────────────────

    /**
     * Load novel from local Room storage. If not yet downloaded, just show metadata
     * from the API and the download button.
     */
    fun loadNovel(novelId: Long) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, novelId = novelId)

            // Try to load from local storage first
            val localNovel = db.offlineNovelDao().getById(novelId)
            val progress = db.localNovelProgressDao().getProgress(novelId)
            val chapters = db.offlineNovelChapterDao().getChapters(novelId)

            if (localNovel != null && chapters.isNotEmpty()) {
                // Fully available offline
                val lastCh = progress?.lastChapter ?: 1
                val startIdx = (lastCh - 1).coerceIn(0, chapters.size - 1)
                val currentCh = chapters[startIdx]

                uiState = uiState.copy(
                    title = localNovel.title,
                    author = localNovel.author,
                    description = localNovel.description,
                    totalChapters = localNovel.totalChapters,
                    chaptersDownloaded = localNovel.chaptersDownloaded,
                    isDownloadCompleted = localNovel.downloadCompleted,
                    chapters = chapters,
                    currentChapterIndex = startIdx,
                    currentChapterBody = currentCh.body,
                    currentChapterTitle = currentCh.title.ifBlank { "Chapter ${currentCh.chapterNumber}" },
                    lastChapter = lastCh,
                    isBookmarked = progress?.bookmarked ?: false,
                    isCompleted = progress?.completed ?: false,
                    isReadyToRead = true,
                    isDownloading = false,
                    isLoading = false
                )
                startAutoSave()
                return@launch
            }

            // Not downloaded — fetch metadata from server, show detail screen
            val repo = com.curio.app.data.repository.NovelRepository()
            repo.getNovel(novelId).fold(
                onSuccess = { detail ->
                    uiState = uiState.copy(
                        title = detail.novel.title,
                        author = detail.novel.author,
                        description = detail.novel.description,
                        totalChapters = detail.novel.totalChapters,
                        isLoading = false,
                        isReadyToRead = false
                    )
                },
                onFailure = { e ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load novel"
                    )
                }
            )

            // Start auto-save in case user reads offline
            startAutoSave()
        }
    }

    /**
     * Start downloading the novel progressively.
     * Chapter 1 is fetched first → isReadyToRead becomes true → user can start reading.
     */
    fun startDownload() {
        val novelId = uiState.novelId
        if (novelId == 0L) return

        uiState = uiState.copy(isDownloading = true, downloadError = null)

        downloadJob = viewModelScope.launch {
            // Pipe DownloadManager progress into our own flow for the detail screen
            val progressJob = launch {
                downloadManager.downloadProgress.collect { allProgress ->
                    allProgress[novelId]?.let { prog ->
                        _downloadProgress.value = prog
                        if (prog.isReadyToRead) {
                            uiState = uiState.copy(isReadyToRead = true)
                        }
                    }
                }
            }

            downloadManager.downloadNovel(novelId)
            progressJob.cancel()

            // After download completes, reload from local storage
            loadNovel(novelId)
        }
    }

    /** Cancel an ongoing download. */
    fun cancelDownload() {
        downloadJob?.cancel()
        uiState = uiState.copy(isDownloading = false)
    }

    // ── Chapter Navigation ──────────────────────────────────

    fun navigateToChapter(chapterNum: Int) {
        val chapters = uiState.chapters
        val index = chapterNum - 1
        if (index < 0 || index >= chapters.size) return

        val ch = chapters[index]
        uiState = uiState.copy(
            currentChapterIndex = index,
            currentChapterBody = ch.body,
            currentChapterTitle = ch.title.ifBlank { "Chapter ${ch.chapterNumber}" }
        )
        saveProgress()
    }

    fun nextChapter() {
        if (uiState.currentChapterIndex < uiState.totalChapters - 1) {
            navigateToChapter(uiState.currentChapterIndex + 2)
        }
    }

    fun previousChapter() {
        if (uiState.currentChapterIndex > 0) {
            navigateToChapter(uiState.currentChapterIndex)
        }
    }

    fun hasNextChapter(): Boolean = uiState.currentChapterIndex < uiState.totalChapters - 1
    fun hasPreviousChapter(): Boolean = uiState.currentChapterIndex > 0

    // ── Local Progress ──────────────────────────────────────

    fun saveProgress(lastPosition: Int = 0) {
        val novelId = uiState.novelId
        if (novelId == 0L) return

        viewModelScope.launch {
            db.localNovelProgressDao().upsert(LocalNovelProgress(
                novelId = novelId,
                lastChapter = uiState.currentChapterIndex + 1,
                lastPosition = lastPosition,
                completed = uiState.isCompleted,
                bookmarked = uiState.isBookmarked,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    fun toggleBookmark() {
        val newState = !uiState.isBookmarked
        uiState = uiState.copy(isBookmarked = newState)
        viewModelScope.launch {
            db.localNovelProgressDao().setBookmarked(
                uiState.novelId, newState, System.currentTimeMillis()
            )
        }
    }

    fun markAsCompleted() {
        uiState = uiState.copy(isCompleted = true)
        viewModelScope.launch {
            db.localNovelProgressDao().markCompleted(
                uiState.novelId, System.currentTimeMillis()
            )
        }
    }

    private fun startAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                saveProgress()
            }
        }
    }

    // ── TTS ─────────────────────────────────────────────────

    fun toggleTts() {
        if (uiState.isPlaying) stopTts() else startTts()
    }

    private fun startTts() {
        val body = uiState.currentChapterBody
        if (body.isBlank()) return

        uiState = uiState.copy(isTtsLoading = true)

        viewModelScope.launch {
            try {
                val api = com.curio.app.data.api.RetrofitClient.api
                val responseBody = api.generateSpeech(
                    com.curio.app.data.model.TtsRequest(
                        text = body,
                        voice = "en-US-JennyNeural"
                    )
                )
                val file = java.io.File.createTempFile("tts_chapter_", ".mp3")
                file.outputStream().use { output ->
                    responseBody.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }
                uiState = uiState.copy(
                    isTtsLoading = false,
                    isPlaying = true,
                    audioFilePath = file.absolutePath
                )
            } catch (e: Exception) {
                uiState = uiState.copy(isTtsLoading = false, isPlaying = false)
            }
        }
    }

    fun stopTts() {
        uiState = uiState.copy(isPlaying = false, audioFilePath = null)
    }

    fun ttsFinished() {
        uiState = uiState.copy(isPlaying = false, audioFilePath = null)
        if (hasNextChapter()) {
            nextChapter()
            startTts()
        }
    }

    // ── Reading Preferences ─────────────────────────────────

    fun increaseFontSize() {
        if (uiState.fontSize < 28)
            uiState = uiState.copy(fontSize = uiState.fontSize + 2)
    }

    fun decreaseFontSize() {
        if (uiState.fontSize > 12)
            uiState = uiState.copy(fontSize = uiState.fontSize - 2)
    }

    fun toggleDarkMode() {
        uiState = uiState.copy(isDarkMode = !uiState.isDarkMode)
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        saveProgress()
    }
}
