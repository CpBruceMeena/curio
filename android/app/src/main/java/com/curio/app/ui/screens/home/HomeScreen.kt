package com.curio.app.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.model.L1Group
import com.curio.app.ui.screens.feed.FeedScreen
import com.curio.app.ui.theme.Error
import com.curio.app.ui.theme.OnSecondaryContainer
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainerHigh
import com.curio.app.viewmodel.FeedViewModel

private fun l1Emoji(name: String): String = when (name) {
    "Facts" -> "\uD83D\uDCD6"
    "Poems" -> "\uD83C\uDFB5"
    "Short Stories" -> "\uD83D\uDCC4"
    "Puzzles" -> "\uD83E\uDDE9"
    else -> "\u2728"
}

private fun l1Gradient(name: String): Pair<Color, Color> = when (name) {
    "Facts" -> Color(0xFF00f4fe) to Color(0xFF0A4A4E)
    "Poems" -> Color(0xFFf472b6) to Color(0xFF4A1A2E)
    "Short Stories" -> Color(0xFF06b6d4) to Color(0xFF0A3A44)
    "Puzzles" -> Color(0xFFf97316) to Color(0xFF4A1E0A)
    else -> Color(0xFFA8CEC8) to Color(0xFF1A2E2A)
}

@Composable
fun HomeScreen(viewModel: FeedViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var activeL1Group by remember { mutableStateOf<String?>(null) }
    var l2SelectedCategoryIds by remember { mutableStateOf(emptySet<Long>()) }

    AnimatedContent(
        targetState = activeL1Group,
        transitionSpec = {
            if (targetState == null) {
                (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
            } else {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
            }
        },
        label = "homeLevel",
        modifier = Modifier.fillMaxSize()
    ) { groupName ->
        if (groupName == null) {
            L1View(
                groups = uiState.l1Groups,
                isLoading = uiState.isLoading,
                error = uiState.error,
                onL1Click = { name ->
                    activeL1Group = name
                    val l1Group = uiState.l1Groups.find { it.name == name }
                    val allSubCategoryIds = l1Group?.categories?.map { it.id }?.toSet()
                    l2SelectedCategoryIds = emptySet()
                    viewModel.loadL1Feed(allSubCategoryIds)
                }
            )
        } else {
            BackHandler { activeL1Group = null }
            if (groupName == "Facts") {
                L2FactsView(
                    group = uiState.l1Groups.find { it.name == "Facts" },
                    selectedCategoryIds = l2SelectedCategoryIds,
                    onToggleCategory = { id ->
                        l2SelectedCategoryIds = if (l2SelectedCategoryIds.contains(id)) {
                            l2SelectedCategoryIds - id
                        } else {
                            l2SelectedCategoryIds + id
                        }
                        val group = uiState.l1Groups.find { it.name == "Facts" }
                        val allFactIds = group?.categories?.map { it.id }?.toSet() ?: emptySet()
                        viewModel.loadL1Feed(
                            if (l2SelectedCategoryIds.isEmpty()) allFactIds else l2SelectedCategoryIds
                        )
                    },
                    onBack = { activeL1Group = null },
                    viewModel = viewModel
                )
            } else {
                L2FeedView(
                    title = groupName,
                    onBack = { activeL1Group = null },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun L1View(groups: List<L1Group>, isLoading: Boolean, error: String?, onL1Click: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text("Curio", style = MaterialTheme.typography.headlineLarge,
                color = Primary, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("One interesting thing at a time.",
                style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (groups.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().height(400.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when {
                    isLoading -> {
                        Text(text = "⏳", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading categories...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = OnSurfaceVariant)
                    }
                    error != null -> {
                        Text(text = "⚠️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Couldn't load categories",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Error)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(error,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center)
                    }
                    else -> {
                        Text(text = "🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No categories yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Check back soon for new discoveries!",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
        } else {
            val sortedGroups = groups.sortedBy { g ->
                when (g.name) { "Facts" -> 0; "Poems" -> 1; "Short Stories" -> 2; "Puzzles" -> 3; else -> 4 }
            }
            sortedGroups.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { group ->
                        L1Card(
                            name = group.name, emoji = l1Emoji(group.name),
                            gradient = l1Gradient(group.name), subCount = group.categories.size,
                            onClick = { onL1Click(group.name) }, modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun L1Card(name: String, emoji: String, gradient: Pair<Color, Color>,
                   subCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(170.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(gradient.first.copy(alpha = 0.25f), gradient.second.copy(alpha = 0.4f))))
            .clickable { onClick() }.padding(20.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(emoji, fontSize = 40.sp)
            Column {
                Text(name, style = MaterialTheme.typography.titleLarge,
                    color = OnSurface, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text("$subCount topics", style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun L2FactsView(group: L1Group?, selectedCategoryIds: Set<Long>,
                        onToggleCategory: (Long) -> Unit, onBack: () -> Unit,
                        viewModel: FeedViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(Surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainerHigh.copy(alpha = 0.5f))
                .clickable { onBack() }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text("\u2190 Back", style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(12.dp))
            Text("Facts", style = MaterialTheme.typography.titleLarge,
                color = OnSurface, fontWeight = FontWeight.Bold)
        }

        if (group != null) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(group.categories) { _, cat ->
                    val isSelected = selectedCategoryIds.contains(cat.id)
                    Box(modifier = Modifier.clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) SecondaryContainer else SurfaceContainerHigh.copy(alpha = 0.3f))
                        .clickable { onToggleCategory(cat.id) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(cat.name, style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) OnSecondaryContainer else OnSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }

        FeedScreen(viewModel = viewModel)
    }
}

@Composable
private fun L2FeedView(title: String, onBack: () -> Unit, viewModel: FeedViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(Surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainerHigh.copy(alpha = 0.5f))
                .clickable { onBack() }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text("\u2190 Back", style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge,
                color = OnSurface, fontWeight = FontWeight.Bold)
        }
        FeedScreen(viewModel = viewModel)
    }
}
