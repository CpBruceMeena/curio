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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.ui.theme.curioColors
import com.curio.app.viewmodel.JournalViewModel
import com.curio.app.viewmodel.WritingPrompt
import com.curio.app.viewmodel.quickStartPrompts

@Composable
fun JournalPromptSelector(
    viewModel: JournalViewModel,
    onDismiss: () -> Unit
) {
    val cc = curioColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cc.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // ── Header ──
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(cc.accentGradientStart.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "\u270D\uFE0F", fontSize = 32.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "What would you like\nto write today?",
            style = MaterialTheme.typography.headlineSmall,
            color = cc.onSurface,
            fontWeight = FontWeight.Bold,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Pick a prompt to get started, or just start writing freely",
            style = MaterialTheme.typography.bodyMedium,
            color = cc.onSurfaceVariant.copy(alpha = 0.7f),
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Prompt grid ──
        quickStartPrompts.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { prompt ->
                    PromptCard(
                        prompt = prompt,
                        onClick = {
                            viewModel.startNewEntryWithPrompt(prompt)
                        },
                        modifier = Modifier.weight(1f),
                        cc = cc
                    )
                }
                if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Quick start blank ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cc.surfaceContainerHigh.copy(alpha = 0.2f))
                .clickable { onDismiss() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Just start writing \uD83D\uDCDD",
                style = MaterialTheme.typography.labelLarge,
                color = cc.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You can always change the entry type later",
            style = MaterialTheme.typography.labelSmall,
            color = cc.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PromptCard(
    prompt: WritingPrompt,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cc: com.curio.app.ui.theme.CurioColors
) {
    val (startColor, endColor) = when (prompt.type) {
        "gratitude" -> Color(0xFFf472b6) to Color(0xFF4A1A2E)
        "reflection" -> Color(0xFF06b6d4) to Color(0xFF0A3A44)
        "task_list" -> Color(0xFFf97316) to Color(0xFF4A1E0A)
        else -> Color(0xFF00f4fe) to Color(0xFF0A4A4E)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(startColor.copy(alpha = 0.12f), endColor.copy(alpha = 0.08f))
                )
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            // Type chip
            val typeLabel = when (prompt.type) {
                "gratitude" -> "Gratitude"
                "reflection" -> "Reflection"
                "task_list" -> "Tasks"
                else -> "Free Write"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(startColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = startColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = prompt.icon,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = prompt.title,
                style = MaterialTheme.typography.titleSmall,
                color = cc.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = prompt.description,
                style = MaterialTheme.typography.bodySmall,
                color = cc.onSurfaceVariant.copy(alpha = 0.7f),
                lineHeight = 16.sp,
                maxLines = 2
            )
        }
    }
}
