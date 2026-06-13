package com.curio.app.ui.screens.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.curio.app.CurioApp
import com.curio.app.ui.components.LoadingStateScreen
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.viewmodel.NovelReaderViewModel

private val coverGradients = listOf(
    listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)),
    listOf(Color(0xFF4A1942), Color(0xFF893168), Color(0xFFB84D8B)),
    listOf(Color(0xFF0F3460), Color(0xFF16213E), Color(0xFF1A5276)),
)

@Composable
fun NovelDetailScreen(
    novelId: Long,
    onChapterClick: (Int) -> Unit = {},
    viewModel: NovelReaderViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    LaunchedEffect(novelId) {
        viewModel.loadNovel(novelId)
    }

    val state = viewModel.uiState
    val downloadProg by viewModel.downloadProgress.collectAsState()

    if (state.isLoading) {
        LoadingStateScreen(message = "Loading novel...")
        return
    }

    val gradientIndex = (novelId % coverGradients.size).toInt()
    val colors = coverGradients[gradientIndex]
    val downloadPct = downloadProg?.let {
        if (it.totalChapters > 0) it.chaptersDownloaded.toFloat() / it.totalChapters else 0f
    } ?: 0f

    // Cover URL comes from the ViewModel state (populated from API or local storage)
    val coverUrl = state.coverImageUrl

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Cover image ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            ) {
                // Load cover image or show gradient placeholder
                if (coverUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(CurioApp.instance)
                            .data(coverUrl)
                            .crossfade(400)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = state.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors)))
                        },
                        error = {
                            CoverHeader(colors, state.title, state.author, onBack)
                        }
                    )
                } else {
                    CoverHeader(colors, state.title, state.author, onBack)
                }

                // Back button always on top of the cover area
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(start = 12.dp, top = 12.dp)
                        .align(Alignment.TopStart)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // ── Metadata bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Author with icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Person, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        state.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Chapter count with icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AutoStories, null,
                        tint = Primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${state.totalChapters} chapters",
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Description ──
            if (state.description.isNotBlank()) {
                Text(
                    state.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Action button ──
            when {
                // Already downloaded → ready to read
                state.isReadyToRead && state.chapters.isNotEmpty() -> {
                    val readingText = if (state.lastChapter > 1) "Continue Reading"
                        else "Start Reading"

                    Button(
                        onClick = { onChapterClick(state.lastChapter) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp).height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondaryContainer,
                            contentColor = Color(0xFF002021)
                        )
                    ) {
                        Icon(Icons.Filled.PlayCircle, null, Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(readingText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    // Background download progress
                    if (state.isDownloading) {
                        Spacer(Modifier.height(12.dp))
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "Downloading chapters...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${downloadProg?.chaptersDownloaded ?: 0}/${downloadProg?.totalChapters ?: state.totalChapters}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Primary
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { downloadPct },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = Primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        }
                    }

                    // Completed badge
                    if (state.isDownloadCompleted) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.DownloadDone, null,
                                tint = Primary, modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${state.totalChapters} chapters downloaded",
                                style = MaterialTheme.typography.bodySmall,
                                color = Primary.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Currently downloading
                state.isDownloading -> {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Button(
                            onClick = { viewModel.cancelDownload() },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecondaryContainer,
                                contentColor = Color(0xFF002021)
                            )
                        ) {
                            Text("Downloading...", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { downloadPct },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${downloadProg?.chaptersDownloaded ?: 0} of ${downloadProg?.totalChapters ?: state.totalChapters} chapters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        if (state.isReadyToRead) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Chapter 1 ready — tap to start reading",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                // Download error
                state.downloadError != null -> {
                    Button(
                        onClick = { viewModel.startDownload() },
                        modifier = Modifier
                            .fillMaxWidth().padding(horizontal = 20.dp).height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondaryContainer,
                            contentColor = Color(0xFF002021)
                        )
                    ) {
                        Icon(Icons.Filled.Download, null, Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Retry Download", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.downloadError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // Not downloaded yet → show download button
                else -> {
                    Button(
                        onClick = { viewModel.startDownload() },
                        modifier = Modifier
                            .fillMaxWidth().padding(horizontal = 20.dp).height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondaryContainer,
                            contentColor = Color(0xFF002021)
                        )
                    ) {
                        Icon(Icons.Filled.Download, null, Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Download (${state.totalChapters} chapters)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Chapter list ──
            if (state.chapters.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                Text(
                    "Chapters",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(12.dp))

                state.chapters.forEachIndexed { index, chapter ->
                    val chNum = chapter.chapterNumber
                    val isCurrent = chNum == state.lastChapter || chNum == state.currentChapterIndex + 1

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.navigateToChapter(chNum)
                                onChapterClick(chNum)
                            }
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Chapter number badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrent) SecondaryContainer.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$chNum",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCurrent) SecondaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                chapter.title.ifBlank { "Chapter $chNum" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrent) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                            Text(
                                "${chapter.readTimeSecs}s read",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(SecondaryContainer)
                            )
                        }
                    }
                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .padding(horizontal = 20.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CoverHeader(
    colors: List<Color>,
    title: String,
    author: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors))
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, "Back",
                tint = Color.White.copy(alpha = 0.8f)
            )
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.AutoStories, null,
                    tint = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    author,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
