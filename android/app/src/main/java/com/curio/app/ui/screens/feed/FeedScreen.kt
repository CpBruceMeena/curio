package com.curio.app.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.ui.theme.Error
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainer
import com.curio.app.viewmodel.FeedViewModel

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = {
        uiState.content.size
    })

    // Jump to a specific content item when feedStartIndex is set
    LaunchedEffect(uiState.feedStartIndex) {
        val index = uiState.feedStartIndex
        if (index != null && index in 0 until uiState.content.size) {
            pagerState.animateScrollToPage(index)
            viewModel.clearFeedStartIndex()
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
            .background(Surface)
    ) {
        when {
            uiState.isLoading && uiState.content.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading curious content...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceVariant
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
                            color = Error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "Network error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            uiState.content.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🔍", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No content yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = OnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Check back soon for new discoveries!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant.copy(alpha = 0.6f)
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        FullPageCard(
                            title = item.title,
                            body = item.body,
                            category = item.categoryName,
                            readTime = item.readTimeSecs,
                            source = item.source
                        )
                    }
                }

                // Page indicator
                if (uiState.content.size > 1) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${uiState.content.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun FullPageCard(
    title: String,
    body: String,
    category: String,
    readTime: Int,
    source: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Category tag at top
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SecondaryContainer.copy(alpha = 0.2f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = category.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title — full, no truncation
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = OnSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Body — takes remaining space, full content visible
            Text(
                text = body,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            // Bottom metadata
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${readTime}s read",
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = source,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
