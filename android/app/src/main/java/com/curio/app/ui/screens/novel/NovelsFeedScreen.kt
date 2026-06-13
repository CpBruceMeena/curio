package com.curio.app.ui.screens.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.curio.app.CurioApp
import com.curio.app.data.model.Novel
import com.curio.app.data.repository.NovelRepository
import com.curio.app.ui.components.ErrorStateScreen
import com.curio.app.ui.components.LoadingStateScreen
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainer
import com.curio.app.ui.theme.SurfaceContainerHigh
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults

// Rich gradient palette for cover placeholders
private val coverGradients = listOf(
    listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)),
    listOf(Color(0xFF4A1942), Color(0xFF893168), Color(0xFFB84D8B)),
    listOf(Color(0xFF0F3460), Color(0xFF16213E), Color(0xFF1A5276)),
    listOf(Color(0xFF2D4059), Color(0xFF1B262C), Color(0xFF3A5A70)),
    listOf(Color(0xFF3D0000), Color(0xFF6A0000), Color(0xFF8B0000)),
    listOf(Color(0xFF1B4332), Color(0xFF2D6A4F), Color(0xFF40916C)),
    listOf(Color(0xFF5C4033), Color(0xFF8B5E3C), Color(0xFFA67B5B)),
    listOf(Color(0xFF2C3E50), Color(0xFF34495E), Color(0xFF4A6B8A)),
    listOf(Color(0xFF6C3483), Color(0xFF8E44AD), Color(0xFFAF7AC5)),
    listOf(Color(0xFF1A5276), Color(0xFF2E86C1), Color(0xFF5DADE2)),
)

@Composable
fun NovelsFeedScreen(
    onNovelClick: (Long) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val repository = remember { NovelRepository() }
    var novels by remember { mutableStateOf<List<Novel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Track downloaded novel IDs
    var downloadedNovels by remember { mutableStateOf<Set<Long>>(emptySet()) }

    LaunchedEffect(Unit) {
        repository.getNovels(page = 1, limit = 50).fold(
            onSuccess = { novels = it.novels; isLoading = false },
            onFailure = { error = it.message; isLoading = false }
        )
        // Load download status from Room
        try {
            val app = CurioApp.instance
            val db = com.curio.app.data.local.JournalDatabase.getInstance(app)
            val all = db.offlineNovelDao().getAll()
            downloadedNovels = all.filter { it.downloadCompleted }.map { it.id }.toSet()
        } catch (_: Exception) {}
    }

    var searchQuery by remember { mutableStateOf("") }

    // Filter novels by search query (title or author)
    val filteredNovels = remember(novels, searchQuery) {
        if (searchQuery.isBlank()) {
            novels
        } else {
            val q = searchQuery.lowercase()
            novels.filter { novel ->
                novel.title.lowercase().contains(q) ||
                novel.author.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        // ── Top bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Novels",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${filteredNovels.size} titles",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 16.dp)
            )
        }

        // ── Search bar ──
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    "Search novels by title or author...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.2f),
            ),
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { /* keyboard dismissed */ }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        when {
            isLoading -> {
                LoadingStateScreen(message = "Loading novels...")
            }
            error != null -> {
                ErrorStateScreen(
                    message = "Couldn't load novels",
                    subMessage = error,
                    onRetry = {
                        scope.launch {
                            isLoading = true; error = null
                            repository.getNovels(page = 1, limit = 50).fold(
                                onSuccess = { novels = it.novels; isLoading = false },
                                onFailure = { error = it.message; isLoading = false }
                            )
                        }
                    },
                    onDismiss = onBack
                )
            }
            else -> {
                if (filteredNovels.isEmpty()) {
                    // ── Empty search state ──
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No novels found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Try a different search term",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredNovels, key = { it.id }) { novel ->
                            NovelCard(
                                novel = novel,
                                isDownloaded = novel.id in downloadedNovels,
                                onClick = { onNovelClick(novel.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelCard(
    novel: Novel,
    isDownloaded: Boolean,
    onClick: () -> Unit
) {
    val gradientIndex = (novel.id % coverGradients.size).toInt()
    val colors = coverGradients[gradientIndex]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        // ── Cover area ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            if (novel.coverImageUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(CurioApp.instance)
                        .data(novel.coverImageUrl)
                        .crossfade(400)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = novel.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(colors)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoStories,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    },
                    error = {
                        CoverPlaceholder(colors = colors)
                    }
                )
            } else {
                CoverPlaceholder(colors = colors)
            }

            // Download badge
            if (isDownloaded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecondaryContainer.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = Color(0xFF002021),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // ── Info section ──
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = novel.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = novel.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoStories,
                    contentDescription = null,
                    tint = Primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${novel.totalChapters} chapters",
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun CoverPlaceholder(colors: List<Color>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AutoStories,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
