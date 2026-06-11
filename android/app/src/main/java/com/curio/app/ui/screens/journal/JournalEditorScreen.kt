package com.curio.app.ui.screens.journal

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.local.EntryType
import com.curio.app.ui.theme.curioColors
import com.curio.app.viewmodel.JournalViewModel

private val moods = listOf(
    "happy" to "😊", "calm" to "😌", "neutral" to "😐",
    "sad" to "😔", "anxious" to "😰", "excited" to "🤩"
)

@Composable
fun JournalEditorScreen(
    viewModel: JournalViewModel,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val cc = curioColors()
    val isNew = state.currentEntry == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cc.surface)
    ) {
        // ── Top bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.cancelEditing()
                onBack()
            }) {
                Icon(
                    Icons.Filled.ArrowBack,
                    "Back",
                    tint = cc.onSurfaceVariant
                )
            }
            Text(
                text = if (isNew) "New Entry" else "Edit Entry",
                style = MaterialTheme.typography.titleMedium,
                color = cc.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            // Save button
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (state.editTitle.isNotBlank() || state.editContent.isNotBlank())
                            cc.accentGradientStart.copy(alpha = 0.15f)
                        else cc.surfaceContainerHigh.copy(alpha = 0.3f)
                    )
                    .clickable(enabled = !state.isSaving) { viewModel.saveEntry() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isSaving) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .width(16.dp)
                                .height(2.dp),
                            color = cc.accentGradientStart,
                            trackColor = Color.Transparent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(
                            Icons.Filled.Check,
                            "Save",
                            tint = if (state.editTitle.isNotBlank() || state.editContent.isNotBlank())
                                cc.accentGradientStart
                            else cc.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (state.isSaving) "Saving..." else "Save",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (state.editTitle.isNotBlank() || state.editContent.isNotBlank())
                            cc.accentGradientStart
                        else cc.onSurfaceVariant.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Error / Success
        if (state.error != null) {
            Text(
                text = state.error!!,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF5252),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // ── Mood picker ──
            Text(
                text = "How are you feeling?",
                style = MaterialTheme.typography.labelLarge,
                color = cc.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                moods.forEach { (key, emoji) ->
                    val isSelected = state.editMood == key
                    Column(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) cc.accentGradientStart.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .clickable { viewModel.updateMood(if (isSelected) null else key) }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
                        Text(
                            text = key.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) cc.accentGradientStart
                                else cc.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Entry type chips ──
            Text(
                text = "Entry type",
                style = MaterialTheme.typography.labelLarge,
                color = cc.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EntryType.entries.forEach { type ->
                    val isSelected = state.editType == type.key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) cc.accentGradientStart.copy(alpha = 0.15f)
                                else cc.surfaceContainerHigh.copy(alpha = 0.3f)
                            )
                            .clickable { viewModel.updateType(type.key) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = type.icon, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) cc.accentGradientStart
                                    else cc.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Title field ──
            OutlinedTextField(
                value = state.editTitle,
                onValueChange = { viewModel.updateTitle(it) },
                placeholder = { Text("Title", color = cc.onSurfaceVariant.copy(alpha = 0.3f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = cc.onSurface,
                    fontWeight = FontWeight.Bold
                ),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cc.accentGradientStart.copy(alpha = 0.3f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = cc.surfaceContainerHigh.copy(alpha = 0.2f),
                    unfocusedContainerColor = cc.surfaceContainerHigh.copy(alpha = 0.1f),
                    cursorColor = cc.accentGradientStart
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Content field ──
            OutlinedTextField(
                value = state.editContent,
                onValueChange = { viewModel.updateContent(it) },
                placeholder = {
                    Text(
                        if (state.editType == "task_list") {
                            """Add tasks, one per line:
- [ ] Task 1
- [x] Completed task"""
                        } else if (state.editType == "gratitude") {
                            "What are you grateful for today?"
                        } else if (state.editType == "reflection") {
                            "What's on your mind? Reflect on your thoughts..."
                        } else {
                            "Write whatever comes to mind..."
                        },
                        color = cc.onSurfaceVariant.copy(alpha = 0.3f),
                        lineHeight = 22.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = cc.onSurfaceVariant.copy(alpha = 0.9f),
                    lineHeight = 24.sp
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cc.accentGradientStart.copy(alpha = 0.3f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = cc.surfaceContainerHigh.copy(alpha = 0.15f),
                    unfocusedContainerColor = cc.surfaceContainerHigh.copy(alpha = 0.08f),
                    cursorColor = cc.accentGradientStart
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Tags field ──
            OutlinedTextField(
                value = state.editTags,
                onValueChange = { viewModel.updateTags(it) },
                placeholder = { Text("Tags (comma-separated)", color = cc.onSurfaceVariant.copy(alpha = 0.3f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = cc.onSurfaceVariant.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cc.accentGradientStart.copy(alpha = 0.3f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = cc.surfaceContainerHigh.copy(alpha = 0.15f),
                    unfocusedContainerColor = cc.surfaceContainerHigh.copy(alpha = 0.08f),
                    cursorColor = cc.accentGradientStart
                )
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
