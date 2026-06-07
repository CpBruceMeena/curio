package com.curio.app.ui.components

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.model.Content
import com.curio.app.ui.theme.OnSecondaryContainer
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.OutlineVariant
import com.curio.app.ui.theme.Primary
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
    ) {
        // Top accent bar based on category
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(categoryAccent(content.categoryName))
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .padding(top = 16.dp),
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

            Spacer(modifier = Modifier.height(10.dp))

            // Body preview
            Text(
                text = content.body,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant.copy(alpha = 0.9f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
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

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = content.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1
                )
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

private fun categoryAccent(categoryName: String): Color {
    return when (categoryName.lowercase()) {
        "science" -> Color(0xFF00F4FE)
        "space" -> Color(0xFF7C3AED)
        "history" -> Color(0xFFE9C400)
        "biology" -> Color(0xFF63F7FF)
        "psychology" -> Color(0xFFC3EAE4)
        "philosophy" -> Color(0xFFFFE16D)
        "physics" -> Color(0xFF00DCE5)
        "startups" -> Color(0xFFFBBF24)
        "ai" -> Color(0xFF818CF8)
        "economics" -> Color(0xFF4ECDC4)
        "nature" -> Color(0xFF34D399)
        "technology" -> Color(0xFF60A5FA)
        else -> Primary
    }
}
