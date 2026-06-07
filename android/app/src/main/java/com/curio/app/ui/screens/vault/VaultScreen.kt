package com.curio.app.ui.screens.vault

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.curio.app.ui.theme.OnPrimary
import com.curio.app.ui.theme.OnSecondaryContainer
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainer
import com.curio.app.ui.theme.SurfaceContainerHigh
import com.curio.app.ui.theme.SurfaceContainerLow

data class Collection(val name: String, val icon: String, val count: Int, val color: Color)

private val sampleCollections = listOf(
    Collection("Favorites", "⭐", 0, Color(0xFFFBBF24)),
    Collection("Startup Lessons", "💡", 0, Color(0xFF4ECDC4)),
    Collection("Quotes", "💬", 0, Color(0xFFA78BFA)),
    Collection("Science Facts", "🔬", 0, Color(0xFF00F4FE)),
    Collection("Space", "🚀", 0, Color(0xFF7C3AED)),
)

@Composable
fun VaultScreen() {
    val repository = remember { ContentRepository() }
    var allContent by remember { mutableStateOf<List<Content>>(emptyList()) }

    LaunchedEffect(Unit) {
        repository.getFeed(page = 1, pageSize = 10).onSuccess { response ->
            allContent = response.content
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
                    text = "Vault",
                    style = MaterialTheme.typography.headlineLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your personal collection of knowledge.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
        }

        // Collections
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Collections",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "0 saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        item {
            if (allContent.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(200.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    SurfaceContainerHigh.copy(alpha = 0.5f),
                                    SurfaceContainerLow.copy(alpha = 0.3f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📚", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No saved content yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Double-tap any card to save it to your vault.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                // Collection chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sampleCollections) { collection ->
                        CollectionChip(collection = collection)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Recent Content section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "From your feed",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        if (allContent.isNotEmpty()) {
            items(allContent.take(5)) { content ->
                VaultContentItem(content = content)
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Start exploring the feed to build your vault!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Bottom spacer for nav bar
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CollectionChip(collection: Collection) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        collection.color.copy(alpha = 0.15f),
                        collection.color.copy(alpha = 0.05f)
                    )
                )
            )
            .clickable { /* Open collection */ }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = collection.icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${collection.count} items",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun VaultContentItem(content: Content) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainerLow.copy(alpha = 0.5f))
            .clickable { /* Open content detail */ }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "📄", fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = content.title,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = content.categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "·", color = OnSurfaceVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${content.readTimeSecs}s read",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        Text(
            text = "🔖",
            fontSize = 18.sp
        )
    }
}
