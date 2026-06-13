package com.curio.app.ui.screens.novel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.app.ui.components.LoadingStateScreen
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.viewmodel.NovelReaderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable

fun NovelReaderScreen(
    novelId: Long,
    initialChapter: Int = 1,
    viewModel: NovelReaderViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    LaunchedEffect(novelId) {
        viewModel.loadNovel(novelId)
        if (initialChapter > 1) {
            viewModel.navigateToChapter(initialChapter)
        }
    }

    val state = viewModel.uiState

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val chunkedPlayer = remember { ChunkedAudioPlayer(context) }

    val scrollState = rememberScrollState()

    // ── Auto-scroll when TTS chunk changes ──────────────────────
    LaunchedEffect(state.currentTtsChunkIndex) {
        val idx = state.currentTtsChunkIndex
        if (idx > 0 && idx < chunkedPlayer.chunks.size && scrollState.maxValue > 0) {
            // Estimate scroll position based on cumulative characters before this chunk
            val totalChars = state.currentChapterBody.length
            val charsBefore = chunkedPlayer.chunkStartPositions.getOrElse(idx) { 0 }
            val ratio = if (totalChars > 0) charsBefore.toFloat() / totalChars else 0f
            val targetScroll = (ratio * scrollState.maxValue).toInt()
            scrollState.animateScrollTo(targetScroll)
        }
    }

    // ── Sentence-level TTS tracking ────────────────────────────
    // Tracks which sentence within the current chunk is being read,
    // using ExoPlayer playback position to estimate sentence progress.
    LaunchedEffect(state.currentTtsChunkIndex) {
        val chunkIdx = state.currentTtsChunkIndex
        if (chunkIdx < 0 || chunkIdx >= chunkedPlayer.chunks.size) return@LaunchedEffect

        val chunkText = chunkedPlayer.chunks[chunkIdx]
        val sentences = chunkText.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (sentences.isEmpty()) return@LaunchedEffect

        // Start with the first sentence highlighted
        viewModel.updateCurrentTtsSentence(0, sentences.size)

        // Wait briefly for ExoPlayer to prepare and have a valid duration
        delay(150)
        val durationMs = chunkedPlayer.player.duration

        if (durationMs > 0 && durationMs < Long.MAX_VALUE) {
            // Track via ExoPlayer position for accurate timing
            while (state.currentTtsChunkIndex == chunkIdx) {
                val currentPos = chunkedPlayer.player.currentPosition.coerceAtLeast(0)
                val progress = (currentPos.toFloat() / durationMs).coerceIn(0f, 1f)

                val totalChars = chunkText.length
                val targetCharOffset = (progress * totalChars).toInt()

                var charAccum = 0
                var sentenceIdx = 0
                for (i in sentences.indices) {
                    charAccum += sentences[i].length
                    if (targetCharOffset <= charAccum) {
                        sentenceIdx = i
                        break
                    }
                    sentenceIdx = i
                }

                viewModel.updateCurrentTtsSentence(sentenceIdx, sentences.size)
                delay(200)
            }
        }
        // If duration isn't available, the first sentence stays highlighted
    }

    // ── Text layout tracking for long-press word selection ─────
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val annotationHighlightColor = if (state.isDarkMode) Color(0xFFF9A825).copy(alpha = 0.25f) else Color(0xFFF9A825).copy(alpha = 0.35f)

    /** Expand from a character offset to find word boundaries. */
    fun findWordRange(text: String, offset: Int): Pair<Int, Int>? {
        if (text.isBlank() || offset !in text.indices) return null
        val boundaryChars = " .,!?;:\"'()[]—…\n\r\t"
        var start = offset
        var end = offset
        while (start > 0 && text[start - 1] !in boundaryChars) start--
        while (end < text.length && text[end] !in boundaryChars) end++
        return if (start == end) null else Pair(start, end)
    }

    // ── Sentence start positions within the body text ────────────
    val sentenceStartPositions = remember(state.currentTtsChunkIndex, chunkedPlayer.chunkStartPositions) {
        val chunkIdx = state.currentTtsChunkIndex
        if (chunkIdx < 0 || chunkIdx >= chunkedPlayer.chunks.size) emptyList()
        else {
            val chunkText = chunkedPlayer.chunks[chunkIdx]
            val body = state.currentChapterBody
            val chunkStart = chunkedPlayer.chunkStartPositions.getOrElse(chunkIdx) { 0 }
            val sentences = chunkText.split(Regex("(?<=[.!?])\\s+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val positions = mutableListOf<Int>()
            var searchFrom = chunkStart
            for (sentence in sentences) {
                val idx = body.indexOf(sentence, searchFrom)
                if (idx >= 0) {
                    positions.add(idx)
                    searchFrom = idx + sentence.length
                } else {
                    positions.add(searchFrom)
                    searchFrom += sentence.length + 1
                }
            }
            positions
        }
    }

    // ── AnnotatedString with highlights (saved annotations + TTS sentence) ──
    // Light mode (warm sepia bg) needs a stronger tint than dark mode
    val ttsHighlightColor = if (state.isDarkMode) Primary.copy(alpha = 0.18f)
        else Primary.copy(alpha = 0.30f)
    val annotatedBody = remember(state.currentChapterBody, state.savedAnnotations, state.currentTtsChunkIndex, state.currentTtsSentenceIndex, sentenceStartPositions) {
        buildAnnotatedString {
            val body = state.currentChapterBody
            if (body.isBlank()) {
                append(body)
                return@buildAnnotatedString
            }

            // Start with full body text
            append(body)

            // 1. Apply saved annotation highlights (amber highlighter)
            for (ann in state.savedAnnotations) {
                if (ann.startPosition in body.indices && ann.endPosition <= body.length) {
                    addStyle(
                        SpanStyle(background = annotationHighlightColor),
                        ann.startPosition,
                        ann.endPosition
                    )
                }
            }

            // 2. Apply TTS sentence highlight on top (overrides annotation in overlapping ranges)
            val chunkIdx = state.currentTtsChunkIndex
            val sentenceIdx = state.currentTtsSentenceIndex
            val positions = sentenceStartPositions
            if (chunkIdx >= 0 && sentenceIdx >= 0 && sentenceIdx < positions.size) {
                val chunkText = chunkedPlayer.chunks.getOrNull(chunkIdx) ?: return@buildAnnotatedString
                val sentences = chunkText.split(Regex("(?<=[.!?])\\s+"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                if (sentenceIdx < sentences.size) {
                    val startPos = positions[sentenceIdx]
                    val endPos = startPos + sentences[sentenceIdx].length
                    if (startPos in body.indices && endPos <= body.length) {
                        addStyle(
                            SpanStyle(background = ttsHighlightColor),
                            startPos,
                            endPos
                        )
                    }
                }
            }
        }
    }

    // Set up ExoPlayer + chunked player lifecycle
    DisposableEffect(Unit) {
        chunkedPlayer.initialize(
            onChunkProgress = { loaded, total ->
                viewModel.updateChunkProgress(loaded, total)
            },
            onCompletion = {
                viewModel.ttsFinished()
                // No auto-advance — user taps Listen per chapter.
            },
            onChunkChanged = { chunkIndex ->
                viewModel.updateCurrentTtsChunk(chunkIndex)
            }
        )
        onDispose {
            chunkedPlayer.release()
        }
    }

    // No LaunchedEffect for TTS — the player is wired directly
    // to the click handler below to avoid circular dependency
    // (LaunchedEffect waiting for chunkTotal > 0, but chunkTotal
    //  only becomes > 0 via player callback)

    if (state.isLoading) {
        LoadingStateScreen(message = "Loading chapter...")
        return
    }

    // Dark mode = app theme colors; Light mode = warm sepia for comfortable reading
    val bgColor = if (state.isDarkMode) MaterialTheme.colorScheme.surface else Color(0xFFFDF8F0)
    val textColor = if (state.isDarkMode) MaterialTheme.colorScheme.onSurface else Color(0xFF2C2C2C)
    val mutedColor = if (state.isDarkMode) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF8C7E72)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Top toolbar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                val isNumberedChapter = state.currentChapterTitle.isBlank() ||
                    state.currentChapterTitle.startsWith("Chapter", ignoreCase = true)
                val currentChNum = if (state.chapters.isNotEmpty() &&
                    state.currentChapterIndex < state.chapters.size
                ) {
                    state.chapters[state.currentChapterIndex].chapterNumber
                } else {
                    state.currentChapterIndex + 1
                }
                val headerText = if (isNumberedChapter) {
                    "Chapter $currentChNum of ${state.totalChapters}"
                } else {
                    state.currentChapterTitle
                }
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            // Dark mode toggle
            IconButton(onClick = { viewModel.toggleDarkMode() }) {
                Icon(
                    imageVector = if (state.isDarkMode) Icons.Filled.LightMode
                        else Icons.Filled.DarkMode,
                    contentDescription = "Toggle theme",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Bookmark toggle
            IconButton(onClick = { viewModel.toggleBookmark() }) {
                Icon(
                    imageVector = if (state.isBookmarked) Icons.Filled.Bookmark
                        else Icons.Filled.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (state.isBookmarked) SecondaryContainer else textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── Chapter content with scroll progress ──
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {

            // Reading progress (ratio 0..1)
            val scrollProgress by remember {
                derivedStateOf {
                    if (scrollState.maxValue > 0) {
                        scrollState.value.toFloat() / scrollState.maxValue
                    } else 0f
                }
            }

            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    // Chapter title in body — always shown as a decorative heading
                    if (state.currentChapterTitle.isNotBlank()) {
                        val isNumberedChapter = state.currentChapterTitle.isBlank() ||
                            state.currentChapterTitle.startsWith("Chapter", ignoreCase = true)
                        if (isNumberedChapter) {
                            // Numbered chapter: show as elegant roman-style heading
                            Text(
                                text = state.currentChapterTitle,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    letterSpacing = 4.sp
                                ),
                                color = textColor.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                            )
                        } else {
                            // Named section: show as strong heading
                            Text(
                                text = state.currentChapterTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }

                    // Thin divider before body
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(mutedColor.copy(alpha = 0.15f))
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Body with annotation highlights + TTS highlight + long-press selection
                    Text(
                        text = annotatedBody,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = state.fontSize.sp,
                            lineHeight = (state.fontSize * state.lineSpacing).sp
                        ),
                        color = textColor,
                        textAlign = TextAlign.Start,
                        onTextLayout = { textLayoutResult = it },
                        modifier = Modifier.pointerInput(state.currentChapterBody, state.savedAnnotations) {
                            detectTapGestures(
                                onTap = { offset ->
                                    textLayoutResult?.let { layout ->
                                        val charOffset = layout.getOffsetForPosition(offset)
                                        // Check if tap is within a saved annotation
                                        val tapped = state.savedAnnotations.firstOrNull { ann ->
                                            charOffset in ann.startPosition until ann.endPosition
                                        }
                                        if (tapped != null) {
                                            viewModel.showAnnotationDetails(tapped)
                                        }
                                    }
                                },
                                onLongPress = { offset ->
                                    textLayoutResult?.let { layout ->
                                        val charOffset = layout.getOffsetForPosition(offset)
                                        val range = findWordRange(state.currentChapterBody, charOffset)
                                        if (range != null) {
                                            val word = state.currentChapterBody.substring(range.first, range.second)
                                            if (word.isNotBlank()) {
                                                viewModel.onTextLongPressed(word, range.first, range.second)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Draggable vertical scrollbar thumb
                val scrollbarMax = scrollState.maxValue.toFloat().coerceAtLeast(1f)
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(8.dp)
                        .padding(start = 2.dp, top = 4.dp, bottom = 4.dp)
                        .pointerInput(scrollState) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val ratio = (offset.y / size.height).coerceIn(0f, 1f)
                                    coroutineScope.launch {
                                        scrollState.scrollTo((ratio * scrollbarMax).toInt().coerceIn(0, scrollState.maxValue))
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val ratio = (change.position.y / size.height).coerceIn(0f, 1f)
                                    coroutineScope.launch {
                                        scrollState.scrollTo((ratio * scrollbarMax).toInt().coerceIn(0, scrollState.maxValue))
                                    }
                                }
                            )
                        }
                ) {
                    val viewH = size.height.coerceAtLeast(1f)
                    val thumbH = (viewH * viewH / (viewH + scrollbarMax))
                        .coerceIn(20f, viewH * 0.8f)
                    val thumbY = scrollProgress * (viewH - thumbH)

                    // Background track (subtle line)
                    drawRoundRect(
                        color = mutedColor.copy(alpha = 0.12f),
                        topLeft = Offset(size.width / 2f - 1f, 0f),
                        size = Size(2f, viewH),
                        cornerRadius = CornerRadius(1f, 1f)
                    )

                    // Draggable thumb
                    drawRoundRect(
                        color = mutedColor.copy(alpha = 0.45f),
                        topLeft = Offset(1f, thumbY),
                        size = Size(size.width - 2f, thumbH),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }

            // Thin reading progress bar at the top
            LinearProgressIndicator(
                progress = { scrollProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter),
                color = Primary.copy(alpha = 0.4f),
                trackColor = Color.Transparent,
            )
        }

        // ── Bottom toolbar ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Chapter navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.dismissAnnotationPopup()
                        chunkedPlayer.stop()
                        viewModel.stopTts()
                        viewModel.previousChapter()
                    },
                    enabled = viewModel.hasPreviousChapter()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Previous chapter",
                        tint = if (viewModel.hasPreviousChapter()) textColor
                            else mutedColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // TTS Play/Pause with chunk progress
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (state.isPlaying) SecondaryContainer.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            when {
                                state.isPlaying -> {
                                    chunkedPlayer.stop()
                                    viewModel.stopTts()
                                }
                                !state.isTtsLoading -> {
                                    viewModel.startTts()
                                    coroutineScope.launch {
                                        chunkedPlayer.start(
                                            state.currentChapterBody,
                                            "en-US-JennyNeural"
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause
                                else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = if (state.isPlaying) SecondaryContainer else Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                state.isTtsLoading && state.chunkTotal > 0 ->
                                    "${state.chunkProgress}/${state.chunkTotal}"
                                state.isTtsLoading -> "Loading..."
                                state.isPlaying -> "Playing..."
                                else -> "Listen"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.isPlaying) SecondaryContainer else Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = {
                        viewModel.dismissAnnotationPopup()
                        chunkedPlayer.stop()
                        viewModel.stopTts()
                        viewModel.nextChapter()
                    },
                    enabled = viewModel.hasNextChapter()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Next chapter",
                        tint = if (viewModel.hasNextChapter()) textColor
                            else mutedColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Playback speed selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f)
                speeds.forEach { speed ->
                    val isActive = state.playbackSpeed == speed
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isActive) Primary.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable {
                                viewModel.setPlaybackSpeed(speed)
                                chunkedPlayer.playbackSpeed = speed
                                // Apply instantly to the current chunk (not just on next chunk)
                                chunkedPlayer.player.setPlaybackSpeed(speed)
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (speed == speed.toInt().toFloat()) "${speed.toInt()}×" else "${speed}×",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) Primary else mutedColor.copy(alpha = 0.6f),
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Font size controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.decreaseFontSize() }) {
                    Icon(
                        imageVector = Icons.Filled.TextDecrease,
                        contentDescription = "Decrease font",
                        tint = mutedColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "${state.fontSize}",
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedColor,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = { viewModel.increaseFontSize() }) {
                    Icon(
                        imageVector = Icons.Filled.TextIncrease,
                        contentDescription = "Increase font",
                        tint = mutedColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // ── Save annotation dialog ──
    if (state.showAnnotationPopup && state.pendingAnnotationText.isNotBlank()) {
        var noteText by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { viewModel.dismissAnnotationPopup() }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = bgColor
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Word chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(annotationHighlightColor)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = state.pendingAnnotationText,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF1A1A1A),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Note input
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text("Add a note...", color = mutedColor) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = mutedColor.copy(alpha = 0.3f),
                            focusedBorderColor = Primary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.dismissAnnotationPopup() }) {
                            Text("Cancel", color = mutedColor)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = { viewModel.saveAnnotation(noteText) }
                        ) {
                            Text("Save", color = Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ── View annotation details dialog ──
    state.viewingAnnotation?.let { annotation ->
        Dialog(onDismissRequest = { viewModel.dismissAnnotationDetails() }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = bgColor
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Word chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(annotationHighlightColor)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = annotation.selectedText,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF1A1A1A),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Note
                    if (annotation.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = annotation.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.deleteAnnotation(annotation.id) }
                        ) {
                            Text("Delete", color = Color(0xFFE53935), fontWeight = FontWeight.Medium)
                        }
                        TextButton(onClick = { viewModel.dismissAnnotationDetails() }) {
                            Text("Close", color = Primary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}