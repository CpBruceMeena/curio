package com.curio.app.ui.screens.novel

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.curio.app.data.api.RetrofitClient
import com.curio.app.data.model.TtsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Chunked TTS audio player — splits text into ~1000-char chunks, fetches each
 * via the TTS API, and plays them sequentially via ExoPlayer with background pre-fetching.
 *
 * Design:
 * 1. Load first chunk → start playing immediately (~2-3s latency)
 * 2. While chunk N plays, pre-fetch chunk N+1 in background
 * 3. When chunk N ends, instantly switch to chunk N+1 (file already ready)
 * 4. Report progress as chunks load
 */
class ChunkedAudioPlayer(
    context: Context,
) {
    private val exoPlayer = ExoPlayer.Builder(context).build()
    private val api = RetrofitClient.api

    /** Current chunk splits — public so the UI can read positions for highlighting */
    var chunks: List<String> = emptyList()
        private set
    /** Start character index of each chunk in the original body text */
    var chunkStartPositions: List<Int> = emptyList()
        private set

    private var loadedFiles = mutableMapOf<Int, File>()
    private var currentIndex = 0
    private var isActive = false
    private var progressCallback: ((loaded: Int, total: Int) -> Unit)? = null
    private var completionCallback: (() -> Unit)? = null
    private var chunkChangedCallback: ((chunkIndex: Int) -> Unit)? = null

    val player: ExoPlayer get() = exoPlayer

    /** Current playback speed (1.0 = normal, 2.0 = double). Applied on each chunk start. */
    var playbackSpeed: Float = 1.0f

    /** Current playback progress: (loadedChunks, totalChunks) */
    var loadedCount: Int = 0
        private set
    var totalCount: Int = 0
        private set

    /**
     * Initialize listeners. Call once from the composable's DisposableEffect.
     */
    fun initialize(
        onChunkProgress: (loaded: Int, total: Int) -> Unit,
        onCompletion: () -> Unit,
        onChunkChanged: ((chunkIndex: Int) -> Unit)? = null
    ) {
        progressCallback = onChunkProgress
        completionCallback = onCompletion
        chunkChangedCallback = onChunkChanged

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && isActive) {
                    playNextChunk()
                }
            }
        })
    }

    /**
     * Start chunked playback of [text] using [voice].
     *
     * Splits the text, fetches chunk 0 immediately, starts playing,
     * then pre-fetches remaining chunks in the background.
     */
    suspend fun start(text: String, voice: String) {
        stop()
        isActive = true

        chunks = splitIntoChunks(text)
        chunkStartPositions = computeChunkPositions(text, chunks)
        totalCount = chunks.size
        loadedFiles.clear()
        loadedFiles = HashMap(chunks.size)
        currentIndex = 0
        loadedCount = 0

        if (chunks.isEmpty()) return
        if (chunks.size == 1) {
            // Single chunk — load and play directly
            val file = fetchChunk(chunks[0], voice)
            loadedFiles[0] = file
            loadedCount = 1
            progressCallback?.invoke(1, 1)
            playAt(0)
            return
        }

        // Load first chunk and start playing immediately
        val firstFile = fetchChunk(chunks[0], voice)
        loadedFiles[0] = firstFile
        loadedCount = 1
        progressCallback?.invoke(1, chunks.size)
        playAt(0)

        // Pre-fetch remaining chunks in parallel
        coroutineScope {
            val jobs = (1 until chunks.size).map { i ->
                async { launchLoadChunk(i, voice) }
            }
            jobs.forEach { it.await() }
        }
    }

    /**
     * Pre-fetch chunk [index] in the current IO context.
     */
    private suspend fun launchLoadChunk(index: Int, voice: String) {
        if (!isActive) return
        val file = withContext(Dispatchers.IO) {
            fetchChunk(chunks[index], voice)
        }
        if (!isActive) return
        loadedFiles[index] = file
        loadedCount++
        progressCallback?.invoke(loadedCount, totalCount)

        // Auto-start this chunk if we're waiting for it (player has finished the previous one)
        if (index == currentIndex + 1 && !exoPlayer.isPlaying && isActive) {
            playAt(index)
        }
    }

    /**
     * Play chunk at [index]. Must already be loaded.
     */
    private fun playAt(index: Int) {
        val file = loadedFiles[index] ?: return
        currentIndex = index
        val mediaItem = MediaItem.fromUri(file.toURI().toString())
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.setPlaybackSpeed(playbackSpeed)
        exoPlayer.play()
        chunkChangedCallback?.invoke(index)
    }

    private fun playNextChunk() {
        val nextIndex = currentIndex + 1
        val file = loadedFiles[nextIndex]
        if (file != null) {
            playAt(nextIndex)
        } else if (nextIndex >= chunks.size) {
            // All chunks played
            isActive = false
            completionCallback?.invoke()
        }
        // If next chunk isn't loaded yet, launchLoadChunk will auto-start it
    }

    /**
     * Stop playback and clean up.
     */
    fun stop() {
        isActive = false
        exoPlayer.stop()
        // Clean up temp files
        loadedFiles.values.forEach { it.delete() }
        loadedFiles.clear()
        chunks = emptyList()
        currentIndex = 0
        loadedCount = 0
        totalCount = 0
    }

    /**
     * Pause playback without cleaning up.
     */
    fun pause() {
        if (exoPlayer.isPlaying) exoPlayer.pause()
    }

    /**
     * Resume playback.
     */
    fun resume() {
        if (!exoPlayer.isPlaying && exoPlayer.playbackState != Player.STATE_IDLE) {
            exoPlayer.play()
        }
    }

    /**
     * Release the underlying ExoPlayer. Call in onDispose.
     */
    fun release() {
        stop()
        exoPlayer.release()
    }

    /**
     * Map each chunk back to its start position in the original [bodyText]
     * by searching for the trimmed chunk text sequentially.
     */
    /**
     * Compute each chunk's start position in the original [bodyText].
     * Falls back to cumulative character count if exact match fails
     * (handles whitespace normalization differences).
     */
    private fun computeChunkPositions(bodyText: String, chunks: List<String>): List<Int> {
        val positions = mutableListOf<Int>()
        var searchFrom = 0
        for (chunk in chunks) {
            val trimmed = chunk.trim()
            val idx = bodyText.indexOf(trimmed, searchFrom)
            if (idx >= 0) {
                positions.add(idx)
                searchFrom = idx + trimmed.length
            } else {
                // Fallback: approximate by cumulative char count
                positions.add(searchFrom)
                // Advance searchFrom so later chunks still get reasonable positions
                searchFrom += trimmed.length + 1
            }
        }
        return positions
    }

    companion object {
        /**
         * Split [text] into chunks at sentence boundaries, each up to ~[maxChars] characters.
         *
         * Preserves sentence integrity — splits only after sentence-ending punctuation
         * (. ! ?) followed by whitespace. If a single sentence exceeds maxChars,
         * it splits at the last space before maxChars.
         */
        fun splitIntoChunks(text: String, maxChars: Int = 1000): List<String> {
            if (text.isBlank()) return emptyList()
            if (text.length <= maxChars) return listOf(text.trim())

            val chunks = mutableListOf<String>()
            // Split on sentence boundaries (period, exclamation, question mark + space/newline)
            val sentences = text.split(Regex("(?<=[.!?])\\s+"))

            val current = StringBuilder()
            for (sentence in sentences) {
                val trimmed = sentence.trim()
                if (trimmed.isEmpty()) continue

                if (current.isNotEmpty() && current.length + trimmed.length > maxChars) {
                    chunks.add(current.toString().trim())
                    current.clear()
                }
                if (current.isNotEmpty()) current.append(' ')
                current.append(trimmed)
            }
            if (current.isNotEmpty()) {
                chunks.add(current.toString().trim())
            }

            // Handle case where a single sentence exceeds maxChars
            val finalChunks = mutableListOf<String>()
            for (chunk in chunks) {
                if (chunk.length <= maxChars) {
                    finalChunks.add(chunk)
                } else {
                    // Force-split long sentence at word boundaries
                    val words = chunk.split(" ")
                    val subCurrent = StringBuilder()
                    for (word in words) {
                        if (subCurrent.isNotEmpty() && subCurrent.length + word.length > maxChars) {
                            finalChunks.add(subCurrent.toString().trim())
                            subCurrent.clear()
                        }
                        if (subCurrent.isNotEmpty()) subCurrent.append(' ')
                        subCurrent.append(word)
                    }
                    if (subCurrent.isNotEmpty()) {
                        finalChunks.add(subCurrent.toString().trim())
                    }
                }
            }

            return finalChunks
        }

        /**
         * Fetch audio for a single chunk from the TTS API.
         */
        suspend fun fetchChunk(text: String, voice: String): File {
            val responseBody = RetrofitClient.api.generateSpeech(
                TtsRequest(text = text, voice = voice)
            )
            val file = File.createTempFile("tts_chunk_", ".mp3")
            responseBody.byteStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return file
        }
    }
}
