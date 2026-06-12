package com.curio.app.ui.screens.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight

import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.local.JournalEntry
import com.curio.app.ui.theme.curioColors
import com.curio.app.viewmodel.JournalViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ── Icons & helpers ──

private val typeIcons = mapOf(
    "free_write" to "\u270D\uFE0F", "gratitude" to "\uD83D\uDE4F",
    "task_list" to "\u2705", "reflection" to "\uD83E\uDD14"
)

private val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

private val monthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

@Composable
fun JournalListScreen(
    viewModel: JournalViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val cc = curioColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cc.surface)
    ) {
        // ── Stats bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val streak = state.writingStreak
            StatCard(
                icon = "\uD83D\uDD25",
                value = "${streak}",
                label = "day streak",
                color = cc.accentGradientStart,
                modifier = Modifier.weight(1f),
                cc = cc
            )
            StatCard(
                icon = "\uD83D\uDCDD",
                value = "${state.totalEntries}",
                label = "total entries",
                color = cc.secondaryContainer,
                modifier = Modifier.weight(1f),
                cc = cc
            )
            StatCard(
                icon = "\uD83D\uDCC5",
                value = "${state.thisMonthEntries}",
                label = "this month",
                color = cc.bookmarkActive,
                modifier = Modifier.weight(1f),
                cc = cc
            )
        }

        // ── Date header + calendar toggle ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
            Text(
                text = dateFormat.format(Date(state.selectedDate)),
                style = MaterialTheme.typography.titleMedium,
                color = cc.onSurface,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.entries.isNotEmpty()) {
                    Text(
                        text = "${state.entries.size} ${if (state.entries.size == 1) "entry" else "entries"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = cc.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                // Calendar toggle button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (viewModel.showCalendar) cc.accentGradientStart.copy(alpha = 0.15f)
                            else cc.surfaceContainerHigh.copy(alpha = 0.3f)
                        )
                        .clickable { viewModel.toggleCalendar() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = if (viewModel.showCalendar) "Hide calendar" else "Show calendar",
                        tint = if (viewModel.showCalendar) cc.accentGradientStart
                            else cc.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Collapsible Calendar ──
        AnimatedVisibility(
            visible = viewModel.showCalendar,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            CalendarCard(
                selectedYear = state.selectedYear,
                selectedMonth = state.selectedMonth,
                selectedDate = state.selectedDate,
                daysWithEntries = state.daysWithEntries,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                onDayClick = { day -> viewModel.selectDate(day) },
                onTodayClick = { viewModel.selectToday() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Entry list (empty or populated) ──
        if (state.entries.isEmpty()) {
            // Refined empty state — inviting the user to write
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Decorative circle
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(40.dp))
                            .background(cc.surfaceContainerHigh.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "\u270D\uFE0F", fontSize = 36.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "What's on your mind?",
                        style = MaterialTheme.typography.titleLarge,
                        color = cc.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the + to start writing\nyour thoughts for today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cc.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.entries, key = { it.id }) { entry ->
                    EntryCard(
                        entry = entry,
                        onClick = { viewModel.selectEntry(entry) },
                        cc = cc
                    )
                }
                // Bottom padding so content doesn't touch the screen bottom
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun CalendarCard(
    selectedYear: Int,
    selectedMonth: Int,
    selectedDate: Long,
    daysWithEntries: Set<Long>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (Int) -> Unit,
    onTodayClick: () -> Unit
) {
    val cc = curioColors()

    val cal = remember(selectedYear, selectedMonth) {
        Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
    val calendarDays = remember(selectedYear, selectedMonth) {
        val days = mutableListOf<Int?>()
        repeat(firstDayOfWeek) { days.add(null) }
        for (d in 1..daysInMonth) days.add(d)
        while (days.size % 7 != 0) days.add(null)
        days
    }
    val selectedDayOfMonth = remember(selectedDate) {
        Calendar.getInstance().apply { timeInMillis = selectedDate }
            .get(Calendar.DAY_OF_MONTH)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(cc.surfaceContainer, cc.surface)
                )
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${monthNames[selectedMonth]} $selectedYear",
                    style = MaterialTheme.typography.titleMedium,
                    color = cc.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape)
                        .clickable { onTodayClick() }
                        .background(cc.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Today, "Today",
                        tint = cc.accentGradientStart, modifier = Modifier.size(16.dp)
                    )
                }
            }
            Row {
                IconButton(onClick = onPreviousMonth, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.ChevronLeft, "Previous", tint = cc.onSurfaceVariant)
                }
                IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.ChevronRight, "Next", tint = cc.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            dayNames.forEach { name ->
                Text(
                    text = name, style = MaterialTheme.typography.labelSmall,
                    color = cc.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        calendarDays.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val isSelected = day == selectedDayOfMonth
                        val dayCal = Calendar.getInstance().apply {
                            set(selectedYear, selectedMonth, day, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val dayBucket = dayCal.timeInMillis / 86400000
                        val hasEntry = daysWithEntries.contains(dayBucket)
                        Box(
                            modifier = Modifier.weight(1f).size(36.dp).clip(CircleShape)
                                .background(
                                    if (isSelected) cc.accentGradientStart.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .clickable { onDayClick(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = day.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) cc.accentGradientStart else cc.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = if (isSelected) 14.sp else 13.sp
                                )
                                if (hasEntry) {
                                    Box(
                                        modifier = Modifier.size(4.dp).clip(CircleShape)
                                            .background(cc.accentGradientStart.copy(alpha = 0.7f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun StatCard(
    icon: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    cc: com.curio.app.ui.theme.CurioColors
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(cc.surfaceContainer, cc.surface.copy(alpha = 0.8f))
                )
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = cc.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = cc.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun EntryCard(
    entry: JournalEntry,
    onClick: () -> Unit,
    cc: com.curio.app.ui.theme.CurioColors
) {
    val typeIcon = typeIcons[entry.entryType] ?: "\uD83D\uDCC4"
    val dateFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val previewText = entry.content.take(120).replace('\n', ' ')

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(cc.surfaceContainer, cc.surface.copy(alpha = 0.8f))
                )
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = typeIcon, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (entry.title.isNotEmpty()) entry.title else "Untitled",
                        style = MaterialTheme.typography.titleSmall,
                        color = cc.onSurface, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormat.format(Date(entry.dateCreated)),
                        style = MaterialTheme.typography.labelSmall,
                        color = cc.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            if (previewText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = previewText + if (entry.content.length > 120) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = cc.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp
                )
            }

            if (entry.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    entry.tags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(cc.surfaceContainerHigh.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag.trim(),
                                style = MaterialTheme.typography.labelSmall,
                                color = cc.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
