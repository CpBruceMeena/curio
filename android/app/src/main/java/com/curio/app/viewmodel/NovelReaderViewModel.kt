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
import com.curio.app.data.local.SavedAnnotation
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
    val coverImageUrl: String = "",
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
    val chunkProgress: Int = 0,      // loaded chunks
    val chunkTotal: Int = 0,          // total chunks
    val currentTtsChunkIndex: Int = -1,  // -1 = not playing; 0+ = which chunk is active
    val currentTtsSentenceIndex: Int = -1,  // which sentence within the current chunk is being read
    val currentTtsSentenceTotal: Int = 0,    // total sentences in the current chunk

    // Saved annotations (word highlight + note)
    val savedAnnotations: List<SavedAnnotation> = emptyList(),
    val showAnnotationPopup: Boolean = false,
    val pendingAnnotationText: String = "",
    val pendingAnnotationStart: Int = 0,
    val pendingAnnotationEnd: Int = 0,
    val viewingAnnotation: SavedAnnotation? = null,

    // Reading preferences
    val fontSize: Int = 18,
    val lineSpacing: Float = 1.6f,
    val isDarkMode: Boolean = false,
    val playbackSpeed: Float = 1.0f  // 1.0, 1.25, 1.5, 2.0
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

    // ── Saved Annotations ─────────────────────────────────────

    /** Load annotations for the current chapter from Room. */
    fun loadAnnotationsForChapter() {
        val novelId = uiState.novelId
        val chapterNum = uiState.currentChapterIndex + 1
        if (novelId == 0L) return
        viewModelScope.launch {
            val annotations = db.savedAnnotationDao().getByChapter(novelId, chapterNum)
            uiState = uiState.copy(savedAnnotations = annotations)
        }
    }

    /** Called when the user long-presses text in the chapter body. */
    fun onTextLongPressed(text: String, startPos: Int, endPos: Int) {
        uiState = uiState.copy(
            pendingAnnotationText = text,
            pendingAnnotationStart = startPos,
            pendingAnnotationEnd = endPos,
            showAnnotationPopup = true
        )
    }

    /** Dismiss the annotation popup. */
    fun dismissAnnotationPopup() {
        uiState = uiState.copy(showAnnotationPopup = false, pendingAnnotationText = "")
    }

    /** Save a new annotation with the user's note (or empty). */
    fun saveAnnotation(note: String) {
        val text = uiState.pendingAnnotationText
        if (text.isBlank()) return
        viewModelScope.launch {
            val annotation = SavedAnnotation(
                novelId = uiState.novelId,
                chapterNumber = uiState.currentChapterIndex + 1,
                selectedText = text,
                note = note.trim(),
                startPosition = uiState.pendingAnnotationStart,
                endPosition = uiState.pendingAnnotationEnd
            )
            db.savedAnnotationDao().insert(annotation)
            // Reload annotations
            val annotations = db.savedAnnotationDao().getByChapter(uiState.novelId, uiState.currentChapterIndex + 1)
            uiState = uiState.copy(
                savedAnnotations = annotations,
                showAnnotationPopup = false,
                pendingAnnotationText = ""
            )
        }
    }

    /** Delete an annotation by its ID. */
    fun deleteAnnotation(id: Long) {
        viewModelScope.launch {
            db.savedAnnotationDao().deleteById(id)
            uiState = uiState.copy(viewingAnnotation = null)
            loadAnnotationsForChapter()
        }
    }

    /** Show details for an existing annotation (triggered by tapping on a highlighted word). */
    fun showAnnotationDetails(annotation: SavedAnnotation) {
        uiState = uiState.copy(viewingAnnotation = annotation)
    }

    /** Dismiss the annotation details view. */
    fun dismissAnnotationDetails() {
        uiState = uiState.copy(viewingAnnotation = null)
    }

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
                    coverImageUrl = localNovel.coverImageUrl,
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
                loadAnnotationsForChapter()
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
                        coverImageUrl = detail.novel.coverImageUrl,
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

    /**
     * Refresh a novel by clearing local data and re-downloading from the backend.
     * The backend re-fetches from Gutenberg and replaces all chapters.
     */
    fun refreshNovel() {
        val novelId = uiState.novelId
        if (novelId == 0L) return

        uiState = uiState.copy(isLoading = true)

        viewModelScope.launch {
            // Clear local storage
            db.offlineNovelChapterDao().deleteByNovelId(novelId)
            db.localNovelProgressDao().deleteByNovelId(novelId)
            db.offlineNovelDao().deleteById(novelId)
            db.savedAnnotationDao().deleteByNovel(novelId)

            // Tell backend to re-fetch from Gutenberg
            val repo = com.curio.app.data.repository.NovelRepository()
            repo.refreshNovel(novelId).onFailure { e ->
                uiState = uiState.copy(
                    isLoading = false,
                    error = "Refresh failed: ${e.message}"
                )
                return@launch
            }

            // Re-download from scratch
            startDownload()
        }
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
        loadAnnotationsForChapter()
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

    /**
     * Start chunked TTS — ChunkedAudioPlayer handles splitting, fetching,
     * and playing. We set initial loading state; callbacks from the player
     * update progress.
     */
    fun startTts() {
        val body = uiState.currentChapterBody
        if (body.isBlank()) return

        uiState = uiState.copy(
            isTtsLoading = true,
            isPlaying = false,
            chunkProgress = 0,
            chunkTotal = 0
        )

        // Actual fetching and playback is handled by ChunkedAudioPlayer
        // in the UI layer (NovelReaderScreen), which calls back here via
        // updateChunkProgress() and onChunksLoaded()
    }

    /** Called by ChunkedAudioPlayer as chunks load */
    fun updateChunkProgress(loaded: Int, total: Int) {
        uiState = uiState.copy(
            chunkProgress = loaded,
            chunkTotal = total,
            isTtsLoading = loaded < total,
            isPlaying = true
        )
    }

    /** Called by ChunkedAudioPlayer when a new chunk starts playing */
    fun updateCurrentTtsChunk(index: Int) {
        uiState = uiState.copy(currentTtsChunkIndex = index, currentTtsSentenceIndex = -1, currentTtsSentenceTotal = 0)
    }

    /** Called by the sentence-tracking loop in NovelReaderScreen */
    fun updateCurrentTtsSentence(index: Int, total: Int) {
        if (index != uiState.currentTtsSentenceIndex || total != uiState.currentTtsSentenceTotal) {
            uiState = uiState.copy(currentTtsSentenceIndex = index, currentTtsSentenceTotal = total)
        }
    }

    /** Called by ChunkedAudioPlayer when all chunks loaded */
    fun onChunksLoaded() {
        uiState = uiState.copy(isTtsLoading = false, isPlaying = true)
    }

    fun stopTts() {
        uiState = uiState.copy(
            isPlaying = false,
            isTtsLoading = false,
            audioFilePath = null,
            chunkProgress = 0,
            chunkTotal = 0,
            currentTtsChunkIndex = -1,
            currentTtsSentenceIndex = -1,
            currentTtsSentenceTotal = 0
        )
    }

    fun ttsFinished() {
        uiState = uiState.copy(isPlaying = false, chunkProgress = 0, chunkTotal = 0, currentTtsChunkIndex = -1, currentTtsSentenceIndex = -1, currentTtsSentenceTotal = 0)
        // No auto-advance — user taps Listen per chapter.
        // (Auto-advance can be added later with a "continuous play" toggle
        //  that properly distinguishes user-initiated stop from natural completion.)
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

    /** Set TTS playback speed. Applied by ChunkedAudioPlayer on each chunk switch. */
    fun setPlaybackSpeed(speed: Float) {
        uiState = uiState.copy(playbackSpeed = speed)
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        saveProgress()
    }
}
