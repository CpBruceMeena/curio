package com.curio.app.ui.screens.feed

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.curio.app.ui.components.CommentSheet
import com.curio.app.ui.components.ErrorStateScreen
import com.curio.app.ui.components.LoadingStateScreen
import com.curio.app.ui.components.getCategoryEmoji
import com.curio.app.ui.theme.curioColors
import com.curio.app.viewmodel.FeedViewModel

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onCategoryChange: (String) -> Unit = {},
    onNavigateToDiscover: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = {
        uiState.content.size
    })

    // Comment sheet state
    var showCommentSheet by remember { mutableStateOf(false) }
    var commentContentId by remember { mutableStateOf<Long?>(null) }
    var commentContentTitle by remember { mutableStateOf("") }

    // Audio player
    val coroutineScope = rememberCoroutineScope()
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // Listen for audio completion to trigger auto-swipe in auto-play mode
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && viewModel.autoPlayEnabled) {
                    coroutineScope.launch {
                        delay(2500) // 2.5s pause before next card
                        val currentPage = pagerState.currentPage
                        val currentPlayingId = viewModel.playingContentId
                        val item = uiState.content.getOrNull(currentPage)
                        // Only auto-swipe if we're still on the same card and it's still playing
                        if (item != null && currentPlayingId == item.id) {
                            val nextPage = currentPage + 1
                            if (nextPage < uiState.content.size) {
                                pagerState.animateScrollToPage(nextPage)
                            } else {
                                // End of feed — stop auto-play
                                viewModel.autoPlayEnabled = false
                            }
                        }
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Play/stop audio when ViewModel state changes
    LaunchedEffect(viewModel.audioFilePath, viewModel.playingContentId) {
        when {
            viewModel.audioFilePath != null && viewModel.playingContentId == null -> {
                exoPlayer.stop()
            }
            viewModel.audioFilePath != null && exoPlayer.playbackState != Player.STATE_READY -> {
                val mediaItem = MediaItem.fromUri(viewModel.audioFilePath!!)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            }
            viewModel.audioFilePath == null -> {
                exoPlayer.stop()
            }
        }
    }

    // When auto-play is enabled, immediately start playing the current card
    LaunchedEffect(viewModel.autoPlayEnabled) {
        if (viewModel.autoPlayEnabled) {
            val currentItem = uiState.content.getOrNull(pagerState.currentPage)
            if (currentItem != null) {
                viewModel.playAudio(currentItem.id)
            }
        }
    }

    // Page-change effects: stop audio on swipe, auto-play in auto-play mode, update category/position
    LaunchedEffect(pagerState.currentPage) {
        val currentItem = uiState.content.getOrNull(pagerState.currentPage)
        if (currentItem != null) {
            // Stop audio if swiped away from the playing item
            if (viewModel.playingContentId != null && viewModel.playingContentId != currentItem.id) {
                viewModel.playingContentId = null
                viewModel.audioFilePath = null
            }
            // In auto-play mode, start playing the new card's audio
            if (viewModel.autoPlayEnabled) {
                viewModel.playAudio(currentItem.id)
            }
            // Update category & save position
            onCategoryChange(currentItem.categoryName)
            viewModel.saveFeedPosition(pagerState.currentPage)
        }
    }

    // Jump to a specific content item when feedStartIndex is set
    LaunchedEffect(uiState.feedStartIndex) {
        val index = uiState.feedStartIndex
        if (index != null && index in 0 until uiState.content.size) {
            pagerState.animateScrollToPage(index)
            viewModel.clearFeedStartIndex()
        }
    }

    // Report current category to the parent (for top bar display)
    LaunchedEffect(uiState.content) {
        if (uiState.content.isNotEmpty()) {
            onCategoryChange(uiState.content[0].categoryName)
        }
    }

    // Load more when approaching last page
    val nearEnd by remember {
        derivedStateOf {
            val total = uiState.content.size
            total > 0 && pagerState.currentPage >= total - 2 &&
            uiState.hasMore && !uiState.isLoading
        }
    }

    LaunchedEffect(nearEnd) {
        if (nearEnd) {
            viewModel.loadMore()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val cc = curioColors()
        when {
            uiState.isLoading && uiState.content.isEmpty() -> {
                LoadingStateScreen(message = "Loading curious content...")
            }
            uiState.error != null && uiState.content.isEmpty() -> {
                ErrorStateScreen(
                    message = "Couldn't load feed",
                    subMessage = uiState.error,
                    onRetry = { viewModel.loadMore() },
                    onDismiss = null
                )
            }
            uiState.content.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ... same empty state as before ...
                    Text(text = "🌟", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nothing here yet!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = cc.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This category is still growing. Explore these while we find more gems for you:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cc.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    val categoryPills = uiState.l1Groups.flatMap { it.categories }.take(6)
                    categoryPills.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { cat ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(cc.secondaryContainer.copy(alpha = 0.15f))
                                        .clickable {
                                            viewModel.setSelectedCategoryIds(setOf(cat.id))
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = getCategoryEmoji(cat.name),
                                            fontSize = 22.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = cc.secondaryContainer.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(cc.secondaryContainer.copy(alpha = 0.2f))
                            .clickable { onNavigateToDiscover() }
                            .padding(horizontal = 28.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "Browse All Categories 🎯",
                            style = MaterialTheme.typography.labelLarge,
                            color = cc.secondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            else -> {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val item = uiState.content[page]
                    val pageOffset by remember {
                        derivedStateOf {
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .graphicsLayer {
                                rotationY = pageOffset * 12f
                                translationX = if (pageOffset != 0f) {
                                    pageOffset * size.width * 0.08f
                                } else 0f
                                shadowElevation = (kotlin.math.abs(pageOffset) * 12f).coerceAtMost(12f)
                                alpha = (1f - kotlin.math.abs(pageOffset) * 0.15f).coerceIn(0.7f, 1f)
                                cameraDistance = 12f * density
                            }
                    ) {
                        FullPageCard(
                            title = item.title,
                            description = item.description,
                            poet = item.poet,
                            body = item.body,
                            readTime = item.readTimeSecs,
                            source = item.source,
                            likes = item.likes,
                            isLiked = viewModel.isLiked(item.id),
                            isBookmarked = viewModel.isBookmarked(item.id),
                            isAudioPlaying = viewModel.playingContentId == item.id,
                            isAudioLoading = viewModel.isAudioLoading,
                            onToggleBookmark = { viewModel.toggleBookmark(item.id) },
                            onToggleLike = { viewModel.toggleLike(item.id) },
                            onToggleAudio = { viewModel.playAudio(item.id) },
                            onComment = {
                                commentContentId = item.id
                                commentContentTitle = item.title
                                showCommentSheet = true
                            }
                        )
                    }
                }
            }
        }

        // Comment sheet (overlays the feed)
        if (showCommentSheet && commentContentId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Scrim — taps here dismiss the sheet
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(cc.scrim)
                        .clickable {
                            showCommentSheet = false
                            commentContentId = null
                        }
                )
                // Sheet content — drawn on top of scrim, touch events naturally consumed by children
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(500.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    CommentSheet(
                        contentTitle = commentContentTitle,
                        comments = viewModel.getCommentsFor(commentContentId!!),
                        isLoading = uiState.commentsLoading.contains(commentContentId!!),
                        isSubmitting = uiState.submittingComment,
                        onLoadComments = { viewModel.loadComments(commentContentId!!) },
                        onAddComment = { text ->
                            viewModel.addComment(commentContentId!!, text)
                        },
                        onDismiss = {
                            showCommentSheet = false
                            commentContentId = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun FullPageCard(
    title: String,
    description: String = "",
    poet: String = "",
    body: String,
    readTime: Int,
    source: String,
    likes: Int = 0,
    isLiked: Boolean = false,
    isBookmarked: Boolean = false,
    isAudioPlaying: Boolean = false,
    isAudioLoading: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    onToggleLike: () -> Unit = {},
    onToggleAudio: () -> Unit = {},
    onComment: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val cc = curioColors()
    var showActions by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(cc.cardGradientStart, cc.cardGradientEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(cc.surfaceContainer)
        ) {
            // Scrollable content area — takes remaining space
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 32.dp, end = 32.dp, top = 32.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Poet name (poems, shayari)
                if (poet.isNotEmpty()) {
                    Text(
                        text = poet,
                        style = MaterialTheme.typography.titleMedium,
                        color = cc.accentGradientStart.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Title — full, no truncation
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = cc.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Body — left-aligned for readability across all content types
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = cc.onSurfaceVariant.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start,
                    lineHeight = 26.sp
                )

                // Description — contextual explanation below the poem
                if (description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        cc.accentGradientStart.copy(alpha = 0.08f),
                                        cc.accentGradientMid.copy(alpha = 0.04f)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = cc.onSurfaceVariant.copy(alpha = 0.85f),
                            textAlign = TextAlign.Start,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Extra spacing so content doesn't hide behind the bottom bar
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom action area — always at the bottom, split into two rows
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                cc.surfaceVariant.copy(alpha = 0.0f),
                                cc.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    )
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            ) {
                // Row 1: Card details — read time + source + toggle button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "${readTime}s read",
                            style = MaterialTheme.typography.labelMedium,
                            color = cc.accentGradientStart,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (source.isNotEmpty()) {
                            Text(
                                text = source,
                                style = MaterialTheme.typography.labelSmall,
                                color = cc.onSurfaceVariant.copy(alpha = 0.4f),
                                textAlign = TextAlign.Start
                            )
                        }
                    }

                    // Toggle button for action actions
                    IconButton(
                        onClick = { showActions = !showActions },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (showActions) Icons.Filled.KeyboardArrowUp
                                else Icons.Filled.MoreHoriz,
                            contentDescription = if (showActions) "Hide actions" else "Show actions",
                            tint = if (showActions) cc.accentGradientStart
                                else cc.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showActions,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))

                        // Divider line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(cc.onSurfaceVariant.copy(alpha = 0.1f))
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Row 2: All action buttons, evenly spaced
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Copy button
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(body))
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy content",
                                    tint = cc.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Play / Pause button
                            IconButton(
                                onClick = { onToggleAudio() },
                                enabled = !isAudioLoading,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAudioPlaying) Icons.Filled.PauseCircle
                                        else Icons.Filled.PlayCircle,
                                    contentDescription = if (isAudioLoading) "Loading audio..."
                                        else if (isAudioPlaying) "Pause" else "Play",
                                    tint = if (isAudioPlaying) cc.accentGradientStart
                                        else cc.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Like button
                            IconButton(
                                onClick = { onToggleLike() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Filled.Favorite
                                        else Icons.Filled.FavoriteBorder,
                                    contentDescription = if (isLiked) "Unlike" else "Like",
                                    tint = if (isLiked) cc.bookmarkActive
                                        else cc.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Comment button
                            IconButton(
                                onClick = onComment,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ModeComment,
                                    contentDescription = "Comments",
                                    tint = cc.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Bookmark button
                            IconButton(
                                onClick = { onToggleBookmark() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark
                                        else Icons.Filled.BookmarkBorder,
                                    contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark",
                                    tint = if (isBookmarked) cc.bookmarkActive
                                        else cc.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Share button
                            IconButton(
                                onClick = {
                                    val shareText = buildString {
                                        appendLine("📖 $title")
                                        appendLine()
                                        appendLine(body)
                                        appendLine()
                                        append("— Curio: One interesting thing at a time.")
                                    }
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(
                                        Intent.createChooser(sendIntent, "Share via Curio")
                                    )
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share",
                                    tint = cc.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Subtle gradient border overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            cc.accentGradientStart.copy(alpha = 0.08f),
                            cc.accentGradientMid.copy(alpha = 0.04f),
                            cc.accentGradientStart.copy(alpha = 0.0f)
                        )
                    )
                )
        )
    }
}
