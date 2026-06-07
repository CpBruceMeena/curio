package com.curio.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.curio.app.data.model.Content
import com.curio.app.ui.theme.OnSecondaryContainer
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.OutlineVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.PrimaryContainer
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.SurfaceContainer
import com.curio.app.ui.theme.Tertiary
import com.curio.app.ui.theme.GlassWhite

@Composable
fun KnowledgeCard(
    content: Content,
    modifier: Modifier = Modifier,
    onLike: () -> Unit = {},
    onSave: () -> Unit = {},
    onShare: () -> Unit = {}
) {
    var isLiked by remember { mutableStateOf(false) }
    var imageLoadFailed by remember { mutableStateOf(false) }
    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.2f else 1f,
        animationSpec = tween(200),
        label = "likeScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
    ) {
        // Background image or fallback gradient
        if (content.imageUrl.isNotBlank() && !imageLoadFailed) {
            AsyncImage(
                model = content.imageUrl,
                contentDescription = content.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { imageLoadFailed = true }
            )
        }

        // Fallback gradient when no image or load failed
        if (content.imageUrl.isBlank() || imageLoadFailed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(categoryGradient(content.categoryName))
            )
        }

        // Gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x000B1514),
                            Color(0xF20B1514)
                        )
                    )
                )
        )

        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Category tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SecondaryContainer)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = content.categoryName.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = content.title,
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Body
            Text(
                text = content.body,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant.copy(alpha = 0.9f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🕐", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${content.readTimeSecs}s read",
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(OutlineVariant)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⭐", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Verified",
                        style = MaterialTheme.typography.labelMedium,
                        color = Tertiary
                    )
                }
            }
        }

        // Right sidebar actions
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                emoji = if (isLiked) "❤️" else "🤍",
                label = if (content.likes > 0) formatCount(content.likes) else "",
                onClick = {
                    isLiked = !isLiked
                    onLike()
                }
            )

            ActionButton(
                emoji = "🔖",
                label = "Save",
                onClick = onSave
            )

            ActionButton(
                emoji = "↗️",
                label = "Share",
                onClick = onShare
            )
        }

        // Progress indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(3.dp)
                .background(SurfaceContainer)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.33f)
                    .height(3.dp)
                    .background(Primary)
            )
        }
    }
}

@Composable
private fun ActionButton(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(GlassWhite)
        ) {
            Text(text = emoji, fontSize = 20.sp)
        }
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000 -> "${count / 1000}k"
        else -> count.toString()
    }
}

private fun categoryGradient(categoryName: String): Brush {
    return when (categoryName.lowercase()) {
        "science" -> Brush.linearGradient(listOf(Color(0xFF0D4F4B), Color(0xFF062E2A)))
        "space" -> Brush.linearGradient(listOf(Color(0xFF1A1A3E), Color(0xFF0B1514)))
        "history" -> Brush.linearGradient(listOf(Color(0xFF3D2E00), Color(0xFF1A1500)))
        "biology" -> Brush.linearGradient(listOf(Color(0xFF0D3D35), Color(0xFF062E2A)))
        "psychology" -> Brush.linearGradient(listOf(Color(0xFF2D1B4E), Color(0xFF1A0D2E)))
        "philosophy" -> Brush.linearGradient(listOf(Color(0xFF3D2E1A), Color(0xFF1A1508)))
        "physics" -> Brush.linearGradient(listOf(Color(0xFF002D3D), Color(0xFF001A24)))
        "startups" -> Brush.linearGradient(listOf(Color(0xFF3D2E00), Color(0xFF1A1400)))
        "ai" -> Brush.linearGradient(listOf(Color(0xFF0D2D4E), Color(0xFF061A33)))
        "economics" -> Brush.linearGradient(listOf(Color(0xFF0D3D35), Color(0xFF062620)))
        "nature" -> Brush.linearGradient(listOf(Color(0xFF0D4F3D), Color(0xFF062E21)))
        "technology" -> Brush.linearGradient(listOf(Color(0xFF001A3D), Color(0xFF000D24)))
        else -> Brush.linearGradient(listOf(Color(0xFF1A2D2B), Color(0xFF0B1514)))
    }
}
