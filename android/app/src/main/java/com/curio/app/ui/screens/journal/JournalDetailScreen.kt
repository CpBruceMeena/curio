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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.local.EntryType
import com.curio.app.data.local.JournalEntry
import com.curio.app.ui.theme.curioColors
import com.curio.app.viewmodel.JournalViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val moodEmojis = mapOf(
    "happy" to "😊", "calm" to "😌", "neutral" to "😐",
    "sad" to "😔", "anxious" to "😰", "excited" to "🤩"
)

private val typeIcons = mapOf(
    "free_write" to "✍️", "gratitude" to "🙏",
    "task_list" to "✅", "reflection" to "🤔"
)

@Composable
fun JournalDetailScreen(
    viewModel: JournalViewModel,
    entry: JournalEntry,
    onBack: () -> Unit,
    onEdit: (JournalEntry) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val cc = curioColors()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy · h:mm a", Locale.getDefault()) }
    val moodEmoji = entry.mood?.let { moodEmojis[it] } ?: "📝"
    val typeIcon = typeIcons[entry.entryType] ?: "📄"
    val typeName = EntryType.entries.find { it.key == entry.entryType }?.displayName ?: "Entry"

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
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = cc.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { onEdit(entry) }) {
                Icon(Icons.Filled.Edit, "Edit", tint = cc.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Filled.Delete, "Delete", tint = Color(0xFFFF5252).copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // ── Header card ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                cc.surfaceContainer,
                                cc.surface.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(cc.accentGradientStart.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = moodEmoji, fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (entry.title.isNotEmpty()) entry.title else "Untitled",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = cc.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = typeIcon, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = typeName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cc.accentGradientStart.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = dateFormat.format(Date(entry.dateCreated)),
                        style = MaterialTheme.typography.bodySmall,
                        color = cc.onSurfaceVariant.copy(alpha = 0.5f)
                    )

                    // Tags
                    if (entry.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            entry.tags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cc.surfaceContainerHigh)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "#${tag.trim()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cc.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Content ──
            if (entry.content.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cc.surfaceContainer.copy(alpha = 0.5f))
                    .padding(20.dp)
                ) {
                    Text(
                        text = entry.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = cc.onSurfaceVariant.copy(alpha = 0.85f),
                        lineHeight = 26.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── Delete confirmation dialog ──
    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = cc.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Delete Entry?",
                    color = cc.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "This action cannot be undone.",
                    color = cc.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                        .clickable {
                            showDeleteConfirm = false
                            viewModel.deleteEntry(entry.id)
                            onBack()
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Delete",
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDeleteConfirm = false }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Cancel",
                        color = cc.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}
