package com.curio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.curio.app.data.repository.ContentRepository
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainerHigh
import kotlinx.coroutines.launch

// Quick emoji reactions for one-tap feedback
private val quickReactions = listOf(
    "😊" to "Love it",
    "👍" to "Good",
    "🤔" to "Confusing",
    "🐛" to "Bug",
    "💡" to "Idea",
    "😞" to "Issue"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit
) {
    val maxChars = 500
    var text by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedReaction by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val repository = remember { ContentRepository() }
    val deviceId = com.curio.app.CurioApp.instance.prefs.deviceUuid
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        sheetState = sheetState,
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Drag handle ──
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(OnSurfaceVariant.copy(alpha = 0.2f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Header row: icon + title + close ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    SecondaryContainer.copy(alpha = 0.3f),
                                    SecondaryContainer.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💬", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Share Feedback",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Help shape Curio's future",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Close button
                IconButton(
                    onClick = { if (!isSubmitting) onDismiss() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = OnSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (submitted) {
                // ── Success state ──
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + scaleIn()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SecondaryContainer.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "✅", fontSize = 36.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Got it! 🎉",
                            style = MaterialTheme.typography.headlineSmall,
                            color = SecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Your feedback means a lot. We review every submission.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecondaryContainer,
                                contentColor = Color(0xFF002021)
                            )
                        ) {
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // ── Quick reaction chips ──
                Text(
                    text = "What's on your mind?",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickReactions.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (emoji, label) ->
                                val isSelected = selectedReaction == label
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) SecondaryContainer.copy(alpha = 0.2f)
                                            else SurfaceContainerHigh.copy(alpha = 0.15f)
                                        )
                                        .clickable {
                                            selectedReaction = if (isSelected) null else label
                                            if (!isSelected && text.isEmpty()) {
                                                // Pre-fill a contextual message
                                                text = when (label) {
                                                    "Love it" -> "I really enjoy using Curio! "
                                                    "Good" -> "Curio is great! "
                                                    "Confusing" -> "I'm finding some parts confusing. "
                                                    "Bug" -> "I found an issue: "
                                                    "Idea" -> "Here's an idea: "
                                                    "Issue" -> "I'm experiencing: "
                                                    else -> ""
                                                }
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = emoji, fontSize = 20.sp)
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) SecondaryContainer
                                            else OnSurfaceVariant.copy(alpha = 0.7f),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Input field ──
                OutlinedTextField(
                    value = text,
                    onValueChange = { newValue ->
                        if (newValue.length <= maxChars) {
                            text = newValue
                        }
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    placeholder = {
                        Text(
                            "Tell us more...",
                            color = OnSurfaceVariant.copy(alpha = 0.4f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryContainer,
                        unfocusedBorderColor = SurfaceContainerHigh,
                        focusedContainerColor = SurfaceContainerHigh.copy(alpha = 0.15f),
                        unfocusedContainerColor = SurfaceContainerHigh.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // ── Char count + error ──
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF5252),
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }
                    Text(
                        text = "${text.length} / $maxChars",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (text.length >= maxChars) Color(0xFFFF5252)
                            else OnSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Submit progress bar ──
                if (isSubmitting) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = SecondaryContainer,
                        trackColor = SurfaceContainerHigh.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Submit button ──
                Button(
                    onClick = {
                        val trimmed = text.trim()
                        if (trimmed.isEmpty() && selectedReaction == null) {
                            errorMessage = "Please share your thoughts or pick a reaction"
                            return@Button
                        }
                        val finalText = if (trimmed.isEmpty() && selectedReaction != null) {
                            "${selectedReaction} (reaction)"
                        } else if (selectedReaction != null) {
                            "[${selectedReaction}] $trimmed"
                        } else {
                            trimmed
                        }
                        isSubmitting = true
                        scope.launch {
                            repository.submitFeedback(finalText, deviceId)
                                .onSuccess {
                                    submitted = true
                                    isSubmitting = false
                                }
                                .onFailure {
                                    errorMessage = "Couldn't send. Please try again."
                                    isSubmitting = false
                                }
                        }
                    },
                    enabled = !isSubmitting && (text.trim().isNotEmpty() || selectedReaction != null),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryContainer,
                        contentColor = Color(0xFF002021),
                        disabledContainerColor = SurfaceContainerHigh.copy(alpha = 0.3f),
                        disabledContentColor = OnSurfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = if (isSubmitting) "Sending..." else "Send Feedback",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
