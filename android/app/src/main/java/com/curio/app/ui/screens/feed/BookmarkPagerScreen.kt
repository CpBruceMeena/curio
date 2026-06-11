package com.curio.app.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.curio.app.ui.components.CommentSheet
import com.curio.app.ui.theme.curioColors
import com.curio.app.viewmodel.FeedViewModel


@Composable
fun BookmarkPagerScreen(
    viewModel: FeedViewModel,
    startIndex: Int,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val bookmarked = uiState.bookmarkedContent
    val cc = curioColors()

    // Comment sheet state
    var showCommentSheet by remember { mutableStateOf(false) }
    var commentContentId by remember { mutableStateOf<Long?>(null) }
    var commentContentTitle by remember { mutableStateOf("") }

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, (bookmarked.size - 1).coerceAtLeast(0)),
        pageCount = { bookmarked.size }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cc.surface)
            .statusBarsPadding()
    ) {
        if (bookmarked.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No bookmarks to show",
                    color = cc.onSurfaceVariant
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = bookmarked[page]
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

            // Top-left back button overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cc.surfaceContainer.copy(alpha = 0.85f))
                    .clickable { onBack() }
                    .padding(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to bookmarks",
                    tint = cc.onSurface
                )
            }

            // Page indicator at top center
            if (bookmarked.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cc.surfaceContainer.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${bookmarked.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = cc.onSurfaceVariant
                    )
                }
            }
        }

        // Comment sheet (overlays the pager)
        if (showCommentSheet && commentContentId != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(cc.scrim)
                        .clickable {
                            showCommentSheet = false
                            commentContentId = null
                        }
                )
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
