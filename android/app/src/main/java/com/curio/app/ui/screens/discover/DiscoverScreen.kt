package com.curio.app.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.model.Category
import com.curio.app.data.model.Content
import com.curio.app.data.repository.ContentRepository
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.PrimaryContainer
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainer
import com.curio.app.ui.theme.SurfaceContainerHigh
import com.curio.app.ui.theme.SurfaceContainerLow

private val categoryColors = listOf(
    Color(0xFF00F4FE), Color(0xFFA8CEC8), Color(0xFFE9C400),
    Color(0xFF63F7FF), Color(0xFFC3EAE4), Color(0xFFFFE16D),
    Color(0xFF00DCE5), Color(0xFF7C3AED), Color(0xFFFF6B6B),
    Color(0xFF4ECDC4), Color(0xFFA78BFA), Color(0xFFFBBF24),
)

@Composable
fun DiscoverScreen(
    onNavigateToContent: (Long) -> Unit = {},
    categories: List<Category> = emptyList(),
    onCategoryClick: (String) -> Unit = {}
) {
    val repository = remember { ContentRepository() }
    var trendingContent by remember { mutableStateOf<List<Content>>(emptyList()) }
    var discoverContent by remember { mutableStateOf<List<Content>>(emptyList()) }

    LaunchedEffect(Unit) {
        repository.getFeed(page = 1, pageSize = 20, random = true).onSuccess { response ->
            trendingContent = response.content.take(5)
        }
        repository.getFeed(page = 1, pageSize = 50, random = true).onSuccess { response ->
            discoverContent = response.content
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Discover",
                    style = MaterialTheme.typography.headlineLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Explore curated knowledge across every topic.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
        }

        // Search bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerHigh.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Search any topic...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        // Browse Categories section
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Browse Categories",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${categories.size} topics",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        // Category Grid (using non-lazy grid since we're inside LazyColumn)
        if (categories.isNotEmpty()) {
            item {
                val chunked = categories.take(9).chunked(3)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chunked.forEach { rowCats ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowCats.forEach { category ->
                                CategoryCard(
                                    name = category.name,
                                    accentColor = categoryColors[((category.id - 1) % categoryColors.size).toInt()],
                                    contentCount = category.contentCount,
                                    onClick = { onCategoryClick(category.name) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill remaining slots if row has fewer than 3 items
                            repeat(3 - rowCats.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Trending section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trending Now",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Most loved",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        item {
            if (trendingContent.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trendingContent) { content ->
                        TrendingCard(
                            title = content.title,
                            category = content.categoryName,
                            likes = content.likes,

                            onClick = { onNavigateToContent(content.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Quick Reads section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Reads",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Under 15 seconds",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        item {
            val quickReads = trendingContent.filter { it.readTimeSecs <= 12 }.ifEmpty {
                discoverContent.filter { it.readTimeSecs <= 12 }.take(3)
            }
            if (quickReads.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickReads.take(3).forEach { content ->
                        QuickReadItem(
                            title = content.title,
                            category = content.categoryName,
                            readTime = content.readTimeSecs,
                            likes = content.likes,
                            onClick = { onNavigateToContent(content.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Bottom spacer for nav bar
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun CategoryCard(
    name: String,
    accentColor: Color,
    contentCount: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(accentColor.copy(alpha = 0.2f), accentColor.copy(alpha = 0.05f))
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) { Text(text = getCategoryIcon(name), fontSize = 18.sp) }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = name, style = MaterialTheme.typography.labelMedium,
                color = OnSurface, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, maxLines = 1
            )
            Text(
                text = "$contentCount items", style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TrendingCard(
    title: String, category: String, likes: Int, onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(240.dp).height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerHigh)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(PrimaryContainer.copy(alpha = 0.3f), SurfaceContainer)))
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SecondaryContainer.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = category.uppercase(), style = MaterialTheme.typography.labelSmall,
                    color = SecondaryContainer, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title, style = MaterialTheme.typography.labelLarge,
                color = OnSurface, fontWeight = FontWeight.Bold, maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "❤️", fontSize = 11.sp)
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = formatCount(likes), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuickReadItem(
    title: String, category: String, readTime: Int, likes: Int, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainerLow.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) { Text(text = "⚡", fontSize = 20.sp) }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title, style = MaterialTheme.typography.bodyMedium,
                color = OnSurface, fontWeight = FontWeight.SemiBold, maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = category, style = MaterialTheme.typography.labelSmall, color = Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "·", color = OnSurfaceVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "${readTime}s", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.6f))
            }
        }
        Text(text = "❤️ ${formatCount(likes)}", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
    }
}

private fun formatCount(count: Int): String {
    return when { count >= 1000 -> "${count / 1000}k"; else -> count.toString() }
}

private fun getCategoryIcon(name: String): String {
    return when (name.lowercase()) {
        "science" -> "🔬"; "space" -> "🚀"; "history" -> "📜"
        "biology" -> "🧬"; "psychology" -> "🧠"; "philosophy" -> "💭"
        "physics" -> "⚛️"; "startups" -> "💡"; "ai" -> "🤖"
        "economics" -> "📊"; "nature" -> "🌲"; "technology" -> "💻"
        else -> "✨"
    }
}
