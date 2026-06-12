package com.curio.app.ui.screens.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PlayCircle
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.app.ui.components.LoadingStateScreen
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainer
import com.curio.app.viewmodel.NovelReaderViewModel

private val coverGradients = listOf(
    listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
    listOf(Color(0xFF4A1942), Color(0xFF893168)),
    listOf(Color(0xFF0F3460), Color(0xFF16213E)),
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Cover with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Brush.verticalGradient(colors))
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.AutoStories, null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            state.title, style = MaterialTheme.typography.headlineMedium,
                            color = Color.White, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.author, style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Action button
            Spacer(Modifier.height(24.dp))

            when {
                // Already downloaded → ready to read
                state.isReadyToRead && state.chapters.isNotEmpty() -> {
                    val readingText = if (state.lastChapter > 1) "Continue Reading"
                        else "Start Reading"

                    Button(
                        onClick = { onChapterClick(state.lastChapter) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryContainer)
                    ) {
                        Icon(Icons.Filled.PlayCircle, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(readingText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    // Download progress indicator
                    if (state.isDownloading) {
                        Spacer(Modifier.height(12.dp))
                        Column(Modifier.padding(horizontal = 24.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    "Downloading ${downloadProg?.chaptersDownloaded ?: 0}/${downloadProg?.totalChapters ?: state.totalChapters}",
                                    style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { downloadPct },
                                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                                color = Primary, trackColor = SurfaceContainer
                            )
                        }
                    }

                    // Completed badge
                    if (state.isDownloadCompleted) {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DownloadDone, null, tint = Primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${state.totalChapters} chapters downloaded",
                                style = MaterialTheme.typography.labelSmall, color = Primary
                            )
                        }
                    }
                }

                // Currently downloading
                state.isDownloading -> {
                    Column(Modifier.padding(horizontal = 24.dp)) {
                        Button(
                            onClick = { viewModel.cancelDownload() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryContainer)
                        ) {
                            Text("Downloading...", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { downloadPct },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Primary, trackColor = SurfaceContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${downloadProg?.chaptersDownloaded ?: 0} of ${downloadProg?.totalChapters ?: state.totalChapters} chapters",
                            style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(Modifier.height(8.dp))
                        if (state.isReadyToRead) {
                            Text(
                                "Chapter 1 ready — tap to start reading",
                                style = MaterialTheme.typography.labelSmall, color = Primary,
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
                            .fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryContainer)
                    ) {
                        Icon(Icons.Filled.Download, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Retry Download", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.downloadError ?: "",
                        style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // Not downloaded yet → show download button
                else -> {
                    Button(
                        onClick = { viewModel.startDownload() },
                        modifier = Modifier
                            .fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryContainer)
                    ) {
                        Icon(Icons.Filled.Download, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Download (${state.totalChapters} chapters)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Description
            if (state.description.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    state.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant.copy(alpha = 0.8f),
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
