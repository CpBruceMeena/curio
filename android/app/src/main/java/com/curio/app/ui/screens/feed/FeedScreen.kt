package com.curio.app.ui.screens.feed

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
    viewModel: FeedViewModel,
    onCategoryChange: (String) -> Unit = {},
    onNavigateToDiscover: () -> Unit = {}
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

    // Report current category to the parent (for top bar display)
    LaunchedEffect(uiState.content) {
        if (uiState.content.isNotEmpty()) {
            onCategoryChange(uiState.content[0].categoryName)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val item = uiState.content.getOrNull(pagerState.currentPage)
        if (item != null) {
            onCategoryChange(item.categoryName)
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🌟", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nothing here yet!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This category is still growing. Explore these while we find more gems for you:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Show suggested categories as clickable pills
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
                                        .background(SecondaryContainer.copy(alpha = 0.15f))
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
                                            color = SecondaryContainer,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SecondaryContainer.copy(alpha = 0.8f),
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

                    // Browse all button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(SecondaryContainer.copy(alpha = 0.2f))
                            .clickable { onNavigateToDiscover() }
                            .padding(horizontal = 28.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "Browse All Categories 🎯",
                            style = MaterialTheme.typography.labelLarge,
                            color = SecondaryContainer,
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        FullPageCard(
                            title = item.title,
                            description = item.description,
                            poet = item.poet,
                            body = item.body,
                            readTime = item.readTimeSecs,
                            source = item.source
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
    description: String = "",
    poet: String = "",
    body: String,
    readTime: Int,
    source: String
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainer)
    ) {            Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Poet name (poems, shayari)
            if (poet.isNotEmpty()) {
                Text(
                    text = poet,
                    style = MaterialTheme.typography.titleMedium,
                    color = Primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Title — full, no truncation
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = OnSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Body — left-aligned for readability across all content types
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant.copy(alpha = 0.9f),
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
                        .background(SecondaryContainer.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant.copy(alpha = 0.85f),
                        textAlign = TextAlign.Start,
                        lineHeight = 20.sp
                    )
                }
            }

            // Bottom metadata + share (left-aligned to match body)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
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
                        textAlign = TextAlign.Start
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
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = OnSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
