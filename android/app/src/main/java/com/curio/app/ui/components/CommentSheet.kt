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
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleLarge,
                color = cc.onSurface,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = cc.onSurfaceVariant
                )
            }
        }

        // Content title reference
        Text(
            text = contentTitle,
            style = MaterialTheme.typography.bodySmall,
            color = cc.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            maxLines = 1
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(cc.outlineVariant.copy(alpha = 0.3f))
        )

        // Comments list
        if (isLoading && comments.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = cc.accentGradientStart,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else if (comments.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "💬", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No comments yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = cc.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Be the first to share your thoughts!",
                        style = MaterialTheme.typography.bodySmall,
                        color = cc.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
            ) {
                items(comments, key = { it.id }) { comment ->
                    CommentItem(comment = comment, cc = cc)
                }
            }
        }

        // Input field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cc.surfaceContainerHighest)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = {
                    Text(
                        text = "Add a comment...",
                        color = cc.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = cc.onSurface),
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
                    unfocusedBorderColor = cc.outlineVariant.copy(alpha = 0.3f),
                    cursorColor = cc.accentGradientStart
                ),
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (commentText.isNotBlank() && !isSubmitting) {
                        onAddComment(commentText.trim())
                        commentText = ""
                    }
                },
                enabled = commentText.isNotBlank() && !isSubmitting,
                modifier = Modifier.size(44.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = cc.accentGradientStart,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Send comment",
                        tint = if (commentText.isNotBlank()) cc.accentGradientStart
                        else cc.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cc.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(cc.accentGradientStart.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName.take(1).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = cc.accentGradientStart,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = cc.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                if (timeAgo.isNotEmpty()) {
                    Text(
                        text = timeAgo,
                        style = MaterialTheme.typography.labelSmall,
                        color = cc.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = cc.onSurfaceVariant.copy(alpha = 0.85f)
            )
        }
    }
}
