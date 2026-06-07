package com.curio.app.ui.screens.feed

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.app.ui.components.KnowledgeCard
import com.curio.app.ui.theme.OnSecondaryContainer
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainerHigh
import com.curio.app.viewmodel.FeedViewModel

@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null &&
            lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3 &&
            uiState.hasMore &&
            !uiState.isLoading
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface.copy(alpha = 0.8f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Menu */ }) {
                Text(text = "☰", fontSize = 20.sp)
            }

            Text(
                text = "Curio",
                style = MaterialTheme.typography.headlineMedium,
                color = Primary,
                fontWeight = FontWeight.ExtraBold
            )

            IconButton(onClick = { /* Search */ }) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = Primary
                )
            }
        }

        // Category Filter Tabs
        val categories = uiState.categories
        if (categories.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryChip(
                    name = "All",
                    isSelected = uiState.selectedCategoryId == null,
                    onClick = { viewModel.selectCategory(null) }
                )
                categories.take(7).forEach { category ->
                    CategoryChip(
                        name = category.name,
                        isSelected = uiState.selectedCategoryId == category.id,
                        onClick = { viewModel.selectCategory(category.id) }
                    )
                }
            }
        }

        // Feed Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                                text = "Try selecting a different category",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(
                            items = uiState.content,
                            key = { it.id }
                        ) { item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(580.dp)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                KnowledgeCard(
                                    content = item,
                                    modifier = Modifier.fillMaxSize(),
                                    onLike = { viewModel.likeContent(item.id) }
                                )
                            }
                        }

                        if (uiState.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Loading more...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) SecondaryContainer else SurfaceContainerHigh.copy(alpha = 0.4f)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) OnSecondaryContainer else OnSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
