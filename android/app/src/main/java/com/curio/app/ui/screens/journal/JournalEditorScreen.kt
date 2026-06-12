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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.local.EntryType
import com.curio.app.ui.theme.curioColors
import com.curio.app.viewmodel.JournalViewModel
import com.curio.app.viewmodel.TaskItem

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
                Icon(Icons.Filled.ArrowBack, "Back", tint = cc.onSurfaceVariant)
            }
            Text(
                text = when (state.editType) {
                    "gratitude" -> "Gratitude Entry"
                    "task_list" -> "Task List"
                    "reflection" -> "Reflection"
                    else -> if (isNew) "New Entry" else "Edit Entry"
                },
                style = MaterialTheme.typography.titleMedium,
                color = cc.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            // Save button
            val hasContent = when (state.editType) {
                "gratitude" -> state.editGratitudeItems.any { it.isNotBlank() }
                "task_list" -> state.editTaskItems.any { it.text.isNotBlank() }
                "reflection" -> state.editReflectionAnswers.any { it.isNotBlank() }
                else -> state.editTitle.isNotBlank() || state.editContent.isNotBlank()
            }
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (hasContent) cc.accentGradientStart.copy(alpha = 0.15f)
                        else cc.surfaceContainerHigh.copy(alpha = 0.3f)
                    )
                    .clickable(enabled = !state.isSaving) { viewModel.saveEntry() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isSaving) {
                        LinearProgressIndicator(
                            modifier = Modifier.width(16.dp).height(2.dp),
                            color = cc.accentGradientStart,
                            trackColor = Color.Transparent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(
                            Icons.Filled.Check, "Save",
                            tint = if (hasContent) cc.accentGradientStart
                                else cc.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (state.isSaving) "Saving..." else "Save",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (hasContent) cc.accentGradientStart
                            else cc.onSurfaceVariant.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

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

            // ── Type-specific content ──
            when (state.editType) {
                "gratitude" -> GratitudeEditor(state.editGratitudeItems, state.editGratitudePrompts, cc, viewModel)
                "task_list" -> TaskListEditor(state.editTaskItems, cc, viewModel)
                "reflection" -> ReflectionEditor(state.editReflectionAnswers, state.editReflectionPrompts, cc, viewModel)
                else -> FreeWriteEditor(state.editTitle, state.editContent, state.editTags, cc, viewModel)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FreeWriteEditor(
    title: String,
    content: String,
    tags: String,
    cc: com.curio.app.ui.theme.CurioColors,
    viewModel: JournalViewModel
) {
    // Title field
    OutlinedTextField(
        value = title,
        onValueChange = { viewModel.updateTitle(it) },
        placeholder = { Text("Title", color = cc.onSurfaceVariant.copy(alpha = 0.3f)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge.copy(
            color = cc.onSurface, fontWeight = FontWeight.Bold
        ),
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        colors = fieldColors(cc)
    )
    Spacer(modifier = Modifier.height(12.dp))

    // Content field
    OutlinedTextField(
        value = content,
        onValueChange = { viewModel.updateContent(it) },
        placeholder = {
            Text(
                "Write whatever comes to mind...",
                color = cc.onSurfaceVariant.copy(alpha = 0.3f), lineHeight = 22.sp
            )
        },
        modifier = Modifier.fillMaxWidth().height(280.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = cc.onSurfaceVariant.copy(alpha = 0.9f), lineHeight = 24.sp
        ),
        shape = RoundedCornerShape(14.dp),
        colors = fieldColors(cc)
    )
    Spacer(modifier = Modifier.height(12.dp))

    // Tags field
    OutlinedTextField(
        value = tags,
        onValueChange = { viewModel.updateTags(it) },
        placeholder = { Text("Tags (comma-separated)", color = cc.onSurfaceVariant.copy(alpha = 0.3f)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = cc.onSurfaceVariant.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(14.dp),
        colors = fieldColors(cc)
    )
}

@Composable
private fun GratitudeEditor(
    items: List<String>,
    prompts: List<String>,
    cc: com.curio.app.ui.theme.CurioColors,
    viewModel: JournalViewModel
) {
    Text(
        text = "What are you grateful for?",
        style = MaterialTheme.typography.titleMedium,
        color = cc.onSurface,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Fill in each prompt to capture today's gratitude",
        style = MaterialTheme.typography.bodySmall,
        color = cc.onSurfaceVariant.copy(alpha = 0.6f)
    )
    Spacer(modifier = Modifier.height(16.dp))

    items.forEachIndexed { index, text ->
        val prompt = prompts.getOrElse(index) { "" }
        // Prompt label
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(cc.accentGradientStart.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = cc.accentGradientStart,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = prompt,
                style = MaterialTheme.typography.labelLarge,
                color = cc.accentGradientStart.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { viewModel.updateGratitudeItem(index, it) },
            placeholder = {
                Text("Write something...", color = cc.onSurfaceVariant.copy(alpha = 0.3f))
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = cc.onSurfaceVariant.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(cc)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TaskListEditor(
    items: List<TaskItem>,
    cc: com.curio.app.ui.theme.CurioColors,
    viewModel: JournalViewModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "My Tasks",
                style = MaterialTheme.typography.titleMedium,
                color = cc.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${items.count { it.done }}/${items.size} completed",
                style = MaterialTheme.typography.bodySmall,
                color = cc.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(cc.accentGradientStart.copy(alpha = 0.15f))
                .clickable { viewModel.addTaskItem() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Add, "Add task",
                    tint = cc.accentGradientStart, modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Add task",
                    style = MaterialTheme.typography.labelMedium,
                    color = cc.accentGradientStart,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "✅", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No tasks yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cc.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "Tap \"Add task\" to get started",
                    style = MaterialTheme.typography.bodySmall,
                    color = cc.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    } else {
        items.forEach { task ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cc.surfaceContainerHigh.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.toggleTaskDone(task.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (task.done) Icons.Filled.CheckCircle
                            else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = if (task.done) "Mark incomplete" else "Mark complete",
                        tint = if (task.done) cc.accentGradientStart
                            else cc.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                OutlinedTextField(
                    value = task.text,
                    onValueChange = { viewModel.updateTaskText(task.id, it) },
                    placeholder = {
                        Text("What needs to be done?", color = cc.onSurfaceVariant.copy(alpha = 0.3f))
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = if (task.done) cc.onSurfaceVariant.copy(alpha = 0.4f)
                            else cc.onSurfaceVariant.copy(alpha = 0.9f),
                        textDecoration = if (task.done) androidx.compose.ui.text.style.TextDecoration.LineThrough
                            else androidx.compose.ui.text.style.TextDecoration.None
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = cc.accentGradientStart
                    )
                )

                IconButton(
                    onClick = { viewModel.deleteTaskItem(task.id) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Close, "Delete task",
                        tint = cc.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun ReflectionEditor(
    answers: List<String>,
    prompts: List<String>,
    cc: com.curio.app.ui.theme.CurioColors,
    viewModel: JournalViewModel
) {
    Text(
        text = "Daily Reflection",
        style = MaterialTheme.typography.titleMedium,
        color = cc.onSurface,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Take a moment to reflect on your day",
        style = MaterialTheme.typography.bodySmall,
        color = cc.onSurfaceVariant.copy(alpha = 0.6f)
    )
    Spacer(modifier = Modifier.height(16.dp))

    answers.forEachIndexed { index, text ->
        val prompt = prompts.getOrElse(index) { "" }
        // Prompt card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(cc.accentGradientStart.copy(alpha = 0.08f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val emoji = when (index) {
                    0 -> "🌟"
                    1 -> "💪"
                    2 -> "📚"
                    else -> "💭"
                }
                Text(text = emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.labelLarge,
                    color = cc.accentGradientStart.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { viewModel.updateReflectionAnswer(index, it) },
            placeholder = {
                Text("Write your thoughts...", color = cc.onSurfaceVariant.copy(alpha = 0.3f))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = cc.onSurfaceVariant.copy(alpha = 0.9f), lineHeight = 22.sp
            ),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(cc)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun fieldColors(cc: com.curio.app.ui.theme.CurioColors) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = cc.accentGradientStart.copy(alpha = 0.3f),
    unfocusedBorderColor = Color.Transparent,
    focusedContainerColor = cc.surfaceContainerHigh.copy(alpha = 0.2f),
    unfocusedContainerColor = cc.surfaceContainerHigh.copy(alpha = 0.1f),
    cursorColor = cc.accentGradientStart
)
