package com.curio.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.model.CommentEntry
import com.curio.app.ui.theme.curioColors
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun CommentSheet(
    contentTitle: String,
    comments: List<CommentEntry>,
    isLoading: Boolean,
    isSubmitting: Boolean,
    onLoadComments: () -> Unit,
    onAddComment: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val cc = curioColors()
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        onLoadComments()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cc.surfaceContainer)
    ) {
        // ── Drag Handle ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(cc.outlineVariant.copy(alpha = 0.4f))
            )
        }

        // ── Header with Gradient Accent ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Gradient accent line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                cc.accentGradientStart,
                                cc.accentGradientMid,
                                cc.accentGradientEnd
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Comments",
                        style = MaterialTheme.typography.titleLarge,
                        color = cc.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (comments.isNotEmpty()) {
                        Text(
                            text = "${comments.size} ${if (comments.size == 1) "response" else "responses"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = cc.accentGradientStart,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Close button with gradient ring
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    cc.accentGradientStart.copy(alpha = 0.12f),
                                    cc.accentGradientMid.copy(alpha = 0.08f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = cc.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ── Content title reference ──
        Text(
            text = contentTitle,
            style = MaterialTheme.typography.bodySmall,
            color = cc.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            maxLines = 1
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            cc.accentGradientStart.copy(alpha = 0.05f),
                            Color.Transparent,
                            cc.outlineVariant.copy(alpha = 0.15f)
                        )
                    )
                )
        )

        // ── Comments List ──
        if (isLoading && comments.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = cc.accentGradientStart,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading responses...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cc.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else if (comments.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Decorative empty state
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        cc.accentGradientStart.copy(alpha = 0.08f),
                                        cc.accentGradientMid.copy(alpha = 0.04f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "💬", fontSize = 36.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "No comments yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = cc.onSurface.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Be the first to share your thoughts on this topic!",
                        style = MaterialTheme.typography.bodySmall,
                        color = cc.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 48.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                items(comments, key = { it.id }) { comment ->
                    CommentItemCard(comment = comment, cc = cc)
                }
            }
        }

        // ── Input Area ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Gradient top border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                cc.accentGradientStart.copy(alpha = 0.08f),
                                Color.Transparent,
                                cc.accentGradientMid.copy(alpha = 0.04f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cc.surfaceContainerHighest)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = {
                        Text(
                            text = "Write a response...",
                            color = cc.onSurfaceVariant.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = cc.onSurface,
                        fontSize = 15.sp
                    ),
                    singleLine = true,
                    enabled = !isSubmitting,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commentText.isNotBlank() && !isSubmitting) {
                                onAddComment(commentText.trim())
                                commentText = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cc.accentGradientStart,
                        unfocusedBorderColor = cc.outlineVariant.copy(alpha = 0.2f),
                        cursorColor = cc.accentGradientStart,
                        focusedContainerColor = cc.surfaceVariant.copy(alpha = 0.15f),
                        unfocusedContainerColor = cc.surfaceVariant.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send button with gradient background
                val isActive = commentText.isNotBlank() && !isSubmitting
                val sendScale by animateFloatAsState(
                    targetValue = if (commentText.isNotBlank()) 1f else 0.92f,
                    animationSpec = tween(200),
                    label = "sendScale"
                )

                val sendButtonBg: Modifier = if (isActive) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                cc.accentGradientStart,
                                cc.accentGradientMid
                            )
                        )
                    )
                } else {
                    Modifier.background(cc.surfaceVariant.copy(alpha = 0.3f))
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .scale(sendScale)
                        .clip(CircleShape)
                        .then(sendButtonBg),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (isActive) {
                                onAddComment(commentText.trim())
                                commentText = ""
                            }
                        },
                        enabled = isActive,
                        modifier = Modifier.size(40.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = cc.onSecondaryContainer,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = "Send",
                                tint = if (isActive) cc.onSecondaryContainer
                                else cc.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItemCard(
    comment: CommentEntry,
    cc: com.curio.app.ui.theme.CurioColors
) {
    val displayName = when {
        comment.email.isNotBlank() -> comment.email.substringBefore("@")
        comment.deviceId.isNotBlank() && comment.deviceId != "anonymous" ->
            "User ${comment.deviceId.takeLast(4)}"
        else -> "Anonymous"
    }

    val timeAgo = remember(comment.createdAt) {
        try {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            )
            var parsed: Long? = null
            for (fmt in formats) {
                try {
                    val sdf = SimpleDateFormat(fmt, Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    parsed = sdf.parse(comment.createdAt)?.time
                    if (parsed != null) break
                } catch (_: Exception) {}
            }
            if (parsed != null) {
                val diff = System.currentTimeMillis() - parsed
                when {
                    diff < 60_000 -> "just now"
                    diff < 3_600_000 -> "${diff / 60_000}m ago"
                    diff < 86_400_000 -> "${diff / 3_600_000}h ago"
                    else -> "${diff / 86_400_000}d ago"
                }
            } else {
                ""
            }
        } catch (_: Exception) { "" }
    }

    // Random gradient color for avatar based on displayName hash
    val avatarColor = remember(displayName) {
        val colors = listOf(
            cc.accentGradientStart,
            cc.accentGradientMid,
            cc.accentGradientEnd,
            cc.secondaryContainer,
            cc.primary
        )
        val idx = kotlin.math.abs(displayName.hashCode()) % colors.size
        colors[idx]
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        cc.surfaceVariant.copy(alpha = 0.2f),
                        cc.surfaceVariant.copy(alpha = 0.08f)
                    )
                )
            )
            .padding(start = 4.dp)
    ) {
        // Left gradient accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .align(Alignment.Top)
                .padding(top = 18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            avatarColor.copy(alpha = 0.6f),
                            avatarColor.copy(alpha = 0.2f)
                        )
                    )
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(avatarColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = avatarColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = cc.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )

                        Text(
                            text = "·",
                            color = cc.onSurfaceVariant.copy(alpha = 0.25f),
                            fontSize = 10.sp
                        )

                        if (timeAgo.isNotEmpty()) {
                            Text(
                                text = timeAgo,
                                style = MaterialTheme.typography.labelSmall,
                                color = cc.onSurfaceVariant.copy(alpha = 0.35f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = cc.onSurfaceVariant.copy(alpha = 0.88f),
                lineHeight = 22.sp,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 14.dp, bottom = 12.dp)
            )
        }
    }
}
