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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.model.Novel
import com.curio.app.data.model.NovelChapter
import com.curio.app.data.repository.NovelRepository
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainer

private val coverGradients = listOf(
    listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
    listOf(Color(0xFF4A1942), Color(0xFF893168)),
    listOf(Color(0xFF0F3460), Color(0xFF16213E)),
)

@Composable
fun NovelDetailScreen(
    novelId: Long,
    onChapterClick: (Int) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val repository = remember { NovelRepository() }
    var novel by remember { mutableStateOf<Novel?>(null) }
    var chapters by remember { mutableStateOf<List<NovelChapter>>(emptyList()) }
    var lastChapter by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(novelId) {
        repository.getNovel(novelId).fold(
            onSuccess = { detail ->
                novel = detail.novel
                chapters = detail.chapters
                lastChapter = detail.progress?.lastChapter ?: 1
                isLoading = false
            },
            onFailure = { isLoading = false }
        )
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Surface).statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading...", color = OnSurfaceVariant)
        }
        return
    }

    val n = novel ?: return
    val gradientIndex = (n.id % coverGradients.size).toInt()
    val colors = coverGradients[gradientIndex]
    val readProgress = if (n.totalChapters > 0) (lastChapter.toFloat() / n.totalChapters) else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .statusBarsPadding()
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Cover + Back button
            item {
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.AutoStories,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = n.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = n.author,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Progress indicator
            if (readProgress > 0f) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Read ${lastChapter} of ${n.totalChapters} chapters",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${(readProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { readProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Primary,
                            trackColor = SurfaceContainer
                        )
                    }
                }
            }

            // Continue Reading / Start Reading button
            item {
                Button(
                    onClick = { onChapterClick(lastChapter) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = if (readProgress > 0f) Icons.Filled.PlayCircle
                            else Icons.Filled.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (readProgress > 0f) "Continue Reading" else "Start Reading",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Description
            if (n.description.isNotBlank()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                        Text(
                            text = n.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant.copy(alpha = 0.8f),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Chapter list header
            item {
                Text(
                    text = "Chapters",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            // Chapter list
            itemsIndexed(chapters) { index, chapter ->
                val chapterNum = index + 1
                val isCurrentChapter = chapterNum == lastChapter

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChapterClick(chapterNum) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chapter number
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isCurrentChapter) SecondaryContainer.copy(alpha = 0.2f)
                                else SurfaceContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$chapterNum",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCurrentChapter) SecondaryContainer else OnSurfaceVariant,
                            fontWeight = if (isCurrentChapter) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chapter.title.ifBlank { "Chapter $chapterNum" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCurrentChapter) SecondaryContainer else OnSurface,
                            fontWeight = if (isCurrentChapter) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (chapter.readTimeSecs > 0) {
                            Text(
                                text = "${chapter.readTimeSecs / 60} min read",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    if (isCurrentChapter) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Current",
                            tint = SecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = OnSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
