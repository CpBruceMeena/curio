package com.curio.app.ui.screens.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.app.ui.components.LoadingStateScreen
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainer
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.viewmodel.NovelReaderViewModel

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

    if (state.isLoading) {
        LoadingStateScreen(message = "Loading chapter...")
        return
    }

    val bgColor = if (state.isDarkMode) Color(0xFF1A1A1A) else Color(0xFFFDF8F0)
    val textColor = if (state.isDarkMode) Color(0xFFE0E0E0) else Color(0xFF2C2C2C)
    val mutedColor = if (state.isDarkMode) Color(0xFF888888) else OnSurfaceVariant

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
                Text(
                    text = state.currentChapterTitle.ifBlank { "Chapter ${state.currentChapterIndex + 1}" },
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "Chapter ${state.currentChapterIndex + 1} of ${state.totalChapters}",
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedColor
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

        // ── Chapter content ──
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Chapter title
                if (state.currentChapterTitle.isNotBlank()) {
                    Text(
                        text = state.currentChapterTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                // Body
                Text(
                    text = state.currentChapterBody,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = state.fontSize.sp,
                        lineHeight = (state.fontSize * state.lineSpacing).sp
                    ),
                    color = textColor,
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
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
                    onClick = { viewModel.previousChapter() },
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

                // TTS Play/Pause
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (state.isPlaying) SecondaryContainer.copy(alpha = 0.15f)
                            else SurfaceContainer)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.toggleTts() }
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
                            text = if (state.isTtsLoading) "Loading..."
                                else if (state.isPlaying) "Playing..." else "Listen",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.isPlaying) SecondaryContainer else Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.nextChapter() },
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
}