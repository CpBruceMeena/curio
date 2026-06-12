package com.curio.app.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.curio.app.CurioApp
import com.curio.app.data.model.Novel
import com.curio.app.data.model.NovelChapter
import com.curio.app.data.model.NovelProgress
import com.curio.app.data.model.NovelProgressRequest
import com.curio.app.data.repository.NovelRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class NovelReaderUiState(
    val novel: Novel? = null,
    val chapters: List<NovelChapter> = emptyList(),
    val currentChapter: NovelChapter? = null,
    val currentChapterIndex: Int = 0,
    val totalChapters: Int = 0,
    val progress: NovelProgress? = null,
    val isLoading: Boolean = true,
    val error: String? = null,

    // TTS
    val isPlaying: Boolean = false,
    val isTtsLoading: Boolean = false,
    val audioFilePath: String? = null,

    // Font settings
    val fontSize: Int = 18,
    val lineSpacing: Float = 1.6f,
    val isDarkMode: Boolean = false
)

class NovelReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NovelRepository()
    private val prefs = (application as CurioApp).prefs

    var uiState by mutableStateOf(NovelReaderUiState())
        private set

    private var autoSaveJob: kotlinx.coroutines.Job? = null

    fun loadNovel(novelId: Long) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            // Load novel detail
            repository.getNovel(novelId, prefs.deviceUuid).fold(
                onSuccess = { detail ->
                    val savedChapter = detail.progress?.lastChapter ?: 1
                    val startIndex = (savedChapter - 1).coerceIn(0, detail.chapters.size - 1)

                    uiState = uiState.copy(
                        novel = detail.novel,
                        chapters = detail.chapters,
                        totalChapters = detail.chapters.size,
                        currentChapterIndex = startIndex,
                        currentChapter = detail.chapters.getOrNull(startIndex),
                        progress = detail.progress,
                        isLoading = false
                    )

                    // Start auto-save timer
                    startAutoSave(novelId)
                },
                onFailure = { e ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load novel"
                    )
                }
            )
        }
    }

    fun navigateToChapter(chapterNum: Int) {
        val index = chapterNum - 1
        val chapters = uiState.chapters
        if (index < 0 || index >= chapters.size) return

        uiState = uiState.copy(
            currentChapterIndex = index,
            currentChapter = chapters[index]
        )

        // Save progress immediately on chapter change
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

    // ── Progress Saving ─────────────────────────────────────

    fun saveProgress(lastPosition: Int = 0) {
        val novelId = uiState.novel?.id ?: return
        val chapterNum = uiState.currentChapterIndex + 1

        viewModelScope.launch {
            repository.updateProgress(
                request = NovelProgressRequest(
                    deviceId = prefs.deviceUuid,
                    lastChapter = chapterNum,
                    lastPosition = lastPosition
                ),
                novelId = novelId
            )
        }
    }

    fun markAsCompleted() {
        val novelId = uiState.novel?.id ?: return
        viewModelScope.launch {
            repository.updateProgress(
                request = NovelProgressRequest(
                    deviceId = prefs.deviceUuid,
                    completed = true
                ),
                novelId = novelId
            )
            uiState = uiState.copy(
                progress = uiState.progress?.copy(completed = true)
            )
        }
    }

    fun toggleBookmark() {
        val novelId = uiState.novel?.id ?: return
        val newBookmarkState = !(uiState.progress?.bookmarked ?: false)

        viewModelScope.launch {
            repository.updateProgress(
                request = NovelProgressRequest(
                    deviceId = prefs.deviceUuid,
                    bookmarked = newBookmarkState
                ),
                novelId = novelId
            )
            uiState = uiState.copy(
                progress = uiState.progress?.copy(bookmarked = newBookmarkState)
            )
        }
    }

    private fun startAutoSave(novelId: Long) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(30_000) // Save every 30 seconds
                saveProgress()
            }
        }
    }

    // ── TTS ─────────────────────────────────────────────────

    fun toggleTts() {
        if (uiState.isPlaying) {
            stopTts()
        } else {
            startTts()
        }
    }

    private fun startTts() {
        val chapter = uiState.currentChapter ?: return
        if (chapter.body.isBlank()) return

        uiState = uiState.copy(isTtsLoading = true)

        viewModelScope.launch {
            try {
                // Pass chapter body text directly to TTS API
                // (novel chapters use their own IDs, not content item IDs)
                val api = com.curio.app.data.api.RetrofitClient.api
                val request = com.curio.app.data.model.TtsRequest(
                    text = chapter.body,
                    voice = "en-US-JennyNeural"
                )
                val responseBody = api.generateSpeech(request)
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
                uiState = uiState.copy(
                    isTtsLoading = false,
                    isPlaying = false
                )
            }
        }
    }

    fun stopTts() {
        uiState = uiState.copy(
            isPlaying = false,
            audioFilePath = null
        )
    }

    fun ttsFinished() {
        // Auto-advance to next chapter when TTS finishes
        uiState = uiState.copy(isPlaying = false, audioFilePath = null)
        if (hasNextChapter()) {
            nextChapter()
            startTts()
        }
    }

    // ── Reading Preferences ─────────────────────────────────

    fun increaseFontSize() {
        if (uiState.fontSize < 28) {
            uiState = uiState.copy(fontSize = uiState.fontSize + 2)
        }
    }

    fun decreaseFontSize() {
        if (uiState.fontSize > 12) {
            uiState = uiState.copy(fontSize = uiState.fontSize - 2)
        }
    }

    fun toggleDarkMode() {
        uiState = uiState.copy(isDarkMode = !uiState.isDarkMode)
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        // Save progress on exit
        saveProgress()
    }
}
