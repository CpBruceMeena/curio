package com.curio.app.ui.screens.feed

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.ui.theme.curioColors
import com.curio.app.viewmodel.FeedViewModel

@Composable
fun BookmarksScreen(
    viewModel: FeedViewModel,
    onContentClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cc = curioColors()

    LaunchedEffect(Unit) {
        viewModel.loadBookmarkedContent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cc.surface)
    ) {
        if (uiState.bookmarkedContent.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "\uD83D\uDCDA", fontSize = 64.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No bookmarks yet",
                    style = MaterialTheme.typography.headlineSmall,
                    color = cc.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap the bookmark icon on any card to save your favorite insights here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cc.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.bookmarkedContent) { content ->
                    BookmarkedItemCard(
                        title = content.title,
                        categoryName = content.categoryName,
                        body = content.body,
                        poet = content.poet,
                        readTime = content.readTimeSecs,
                        onClick = { onContentClick(content.id) }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun BookmarkedItemCard(
    title: String,
    categoryName: String,
    body: String,
    poet: String = "",
    readTime: Int = 0,
    onClick: () -> Unit = {}
) {
    val cc = curioColors()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(cc.cardGradientStart, cc.cardGradientEnd)
                )
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 1.dp, end = 1.dp, top = 1.dp, bottom = 1.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(cc.surfaceContainerHigh)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(cc.accentGradientStart.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = categoryName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = cc.accentGradientStart,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(text = "\uD83D\uDCCD", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = cc.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (poet.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = poet,
                    style = MaterialTheme.typography.labelMedium,
                    color = cc.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = cc.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${readTime}s read",
                style = MaterialTheme.typography.labelSmall,
                color = cc.accentGradientStart,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
