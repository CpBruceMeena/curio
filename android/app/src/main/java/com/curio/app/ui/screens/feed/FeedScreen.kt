package com.curio.app.ui.screens.feed

import android.content.Intent
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
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.curio.app.ui.components.CommentSheet
import com.curio.app.ui.theme.curioColors
import com.curio.app.viewmodel.FeedViewModel

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onCategoryChange: (String) -> Unit = {},
    onNavigateToDiscover: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = {
        uiState.content.size
    })

    // Comment sheet state
    var showCommentSheet by remember { mutableStateOf(false) }
    var commentContentId by remember { mutableStateOf<Long?>(null) }
    var commentContentTitle by remember { mutableStateOf("") }

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

    // Update category name and save feed position on page change
    LaunchedEffect(pagerState.currentPage) {
        val item = uiState.content.getOrNull(pagerState.currentPage)
        if (item != null) {
            onCategoryChange(item.categoryName)
            viewModel.saveFeedPosition(pagerState.currentPage)
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading curious content...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = cc.onSurfaceVariant
                    )
                }
            }
            uiState.error != null && uiState.content.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "⚠️", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Couldn't load feed",
                            style = MaterialTheme.typography.bodyLarge,
                            color = cc.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "Network error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cc.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
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
                                            text = cat.name.take(2).uppercase(),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = cc.secondaryContainer,
                                            fontWeight = FontWeight.ExtraBold
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
                            onToggleBookmark = { viewModel.toggleBookmark(item.id) },
                            onToggleLike = { viewModel.toggleLike(item.id) },
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
    onToggleBookmark: () -> Unit = {},
    onToggleLike: () -> Unit = {},
    onComment: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val cc = curioColors()

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

            // Bottom action bar — always at the bottom
            Row(
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
                    .padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: read time + source
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "${readTime}s read",
                        style = MaterialTheme.typography.labelMedium,
                        color = cc.accentGradientStart,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = source,
                        style = MaterialTheme.typography.labelSmall,
                        color = cc.onSurfaceVariant.copy(alpha = 0.4f),
                        textAlign = TextAlign.Start
                    )
                }

                // Right: action buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Copy button
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(body))
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy content",
                            tint = cc.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Like button
                    IconButton(
                        onClick = { onToggleLike() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite
                                else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isLiked) "Unlike" else "Like",
                            tint = if (isLiked) cc.bookmarkActive
                                else cc.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Comment button
                    IconButton(
                        onClick = onComment,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ModeComment,
                            contentDescription = "Comments",
                            tint = cc.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Bookmark button
                    IconButton(
                        onClick = { onToggleBookmark() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark
                                else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark",
                            tint = if (isBookmarked) cc.bookmarkActive
                                else cc.onSurfaceVariant.copy(alpha = 0.6f)
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
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = cc.onSurfaceVariant.copy(alpha = 0.6f)
                        )
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
