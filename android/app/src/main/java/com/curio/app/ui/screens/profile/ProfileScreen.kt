package com.curio.app.ui.screens.profile

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
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.PrimaryContainer
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainer
import com.curio.app.ui.theme.SurfaceContainerHigh
import com.curio.app.ui.theme.SurfaceContainerLow
import com.curio.app.ui.theme.Tertiary
import com.curio.app.ui.theme.TertiaryContainer

data class Stat(val label: String, val value: String, val icon: String)
data class Achievement(val name: String, val icon: String, val unlocked: Boolean, val progress: String)
data class SettingItem(val name: String, val icon: String, val description: String)

private val stats = listOf(
    Stat("Cards Read", "0", "📖"),
    Stat("Saved", "0", "🔖"),
    Stat("Streak", "0 days", "🔥"),
    Stat("Curiosity Score", "0", "💎"),
)

private val achievements = listOf(
    Achievement("First Discovery", "🌱", false, "Read 1 card"),
    Achievement("Curious Mind", "🧠", false, "Read 10 cards"),
    Achievement("Knowledge Seeker", "📚", false, "Read 50 cards"),
    Achievement("Streak Starter", "🔥", false, "3-day streak"),
    Achievement("Dedicated", "💪", false, "7-day streak"),
    Achievement("Scholar", "🎓", false, "Read 100 cards"),
    Achievement("Collector", "📦", false, "Save 10 cards"),
    Achievement("Explorer", "🧭", false, "Browse all categories"),
)

private val settingsItems = listOf(
    SettingItem("Notifications", "🔔", "Daily digest & reminders"),
    SettingItem("Appearance", "🎨", "Theme & display options"),
    SettingItem("Reading Preferences", "📝", "Content & language settings"),
    SettingItem("Data & Storage", "💾", "Cache & offline content"),
    SettingItem("About", "ℹ️", "Version 1.0.0"),
)

@Composable
fun ProfileScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        // Profile Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    SecondaryContainer.copy(alpha = 0.3f),
                                    Primary.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "◇", fontSize = 36.sp, color = SecondaryContainer)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Explorer",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Level 1 · Knowledge Explorer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
        }

        // Stats Grid
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.take(2).forEach { StatCard(stat = it, modifier = Modifier.fillMaxWidth()) }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.drop(2).forEach { StatCard(stat = it, modifier = Modifier.fillMaxWidth()) }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Level Progress
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Next Level",
                        style = MaterialTheme.typography.labelLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "0 / 10 cards",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(SurfaceContainerHigh)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(colors = listOf(Primary, SecondaryContainer))
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Achievements
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Achievements",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "0 / ${achievements.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        // Achievement badges (non-lazy grid)
        item {
            val chunked = achievements.chunked(4)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chunked.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { achievement ->
                            AchievementBadge(
                                achievement = achievement,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Settings
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(settingsItems) { setting ->
            SettingRow(setting = setting)
        }

        // Bottom spacer
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun StatCard(stat: Stat, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow.copy(alpha = 0.5f))
            .padding(14.dp)
    ) {
        Column {
            Text(text = stat.icon, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stat.value,
                style = MaterialTheme.typography.headlineSmall,
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = stat.label,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AchievementBadge(achievement: Achievement, modifier: Modifier = Modifier) {
    val bgModifier = if (achievement.unlocked) {
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(TertiaryContainer.copy(alpha = 0.2f), SurfaceContainer)
                )
            )
            .padding(8.dp)
    } else {
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerHigh.copy(alpha = 0.3f))
            .padding(8.dp)
    }
    Column(
        modifier = bgModifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = achievement.icon,
            fontSize = 24.sp,
            color = OnSurfaceVariant.copy(alpha = if (achievement.unlocked) 1f else 0.4f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = achievement.progress,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant.copy(alpha = if (achievement.unlocked) 0.8f else 0.4f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun SettingRow(setting: SettingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate to setting */ }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = setting.icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = setting.name,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = setting.description,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        Text(text = "›", fontSize = 20.sp, color = OnSurfaceVariant.copy(alpha = 0.4f))
    }
}
