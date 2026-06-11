package com.curio.app.ui.screens.discover

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.model.Content
import com.curio.app.ui.theme.curioColors
import com.curio.app.CurioApp
import com.curio.app.ui.components.getCategoryEmoji
import com.curio.app.viewmodel.FeedViewModel

// Map category names to puzzle types for puzzle navigation
private val PUZZLE_CATEGORIES = mapOf(
    "Sudoku" to "sudoku",
    "Math Puzzles" to "math",
    "Logic Puzzles" to "logic",
    "Word Puzzles" to "word",
    "Mixed Puzzles" to ""
)

private fun getPuzzleType(categoryName: String): String? = PUZZLE_CATEGORIES[categoryName]

@Composable
fun DiscoverScreen(
    viewModel: FeedViewModel,
    onApplyFilter: () -> Unit = {},
    onCategoryClick: (Long) -> Unit = {},
    onPuzzleNavigate: (categoryId: Long, puzzleType: String) -> Unit = { _, _ -> },
    onContentClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingCategoryIds by remember { mutableStateOf(uiState.selectedCategoryIds) }
    // Selected L1 group — shows its subcategories when non-null
    var selectedL1 by remember { mutableStateOf<String?>("Facts") }

    // Refresh discover content and categories every time this screen is composed
    LaunchedEffect(Unit) {
        viewModel.refreshDiscover()
    }

    // Filtered categories based on selected L1
    val displayCategories = if (selectedL1 != null) {
        val group = uiState.l1Groups.find { it.name == selectedL1 }
        group?.categories ?: emptyList()
    } else {
        // "All Categories" — only show Facts subcategories (Poems/Short Stories/Puzzles have their own L1 pills)
        uiState.categories.filter { cat ->
            cat.l1Category.isNullOrEmpty() || cat.l1Category == "Facts"
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        // L1 Category pills
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val l1Groups = uiState.l1Groups
                l1Groups.forEach { group ->
                    val cc = curioColors()
                    val isActive = selectedL1 == group.name
                    val l1Bg = if (isActive) Brush.horizontalGradient(
                        colors = listOf(cc.accentGradientStart, cc.accentGradientMid)
                    ) else androidx.compose.ui.graphics.SolidColor(cc.surfaceContainerHigh.copy(alpha = 0.3f))
                    Box(modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(l1Bg)
                        .clickable {
                            selectedL1 = if (isActive) null else group.name
                            pendingCategoryIds = emptySet()
                        }
                        .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isActive) cc.onSecondaryContainer else cc.onSurfaceVariant,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Subcategories header
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            val cc = curioColors()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedL1 != null) "$selectedL1 Topics" else "All Categories",
                    style = MaterialTheme.typography.titleLarge,
                    color = cc.onSurface, fontWeight = FontWeight.Bold
                )
                if (pendingCategoryIds.isNotEmpty()) {
                    Text("Clear", style = MaterialTheme.typography.labelMedium,
                        color = cc.primary, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { pendingCategoryIds = emptySet() })
                }
            }
        }

        // Category chips (filtered by L1) — 3 columns
        if (displayCategories.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayCategories.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { category ->
                                val puzzleType = getPuzzleType(category.name)
                                if (puzzleType != null) {
                                    CategoryChip(
                                        name = category.name,
                                        isSelected = false,
                                        onClick = {
                                            val catId = if (puzzleType.isEmpty()) 0L else category.id
                                            onPuzzleNavigate(catId, puzzleType)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    CategoryChip(
                                        name = category.name,
                                        isSelected = pendingCategoryIds.contains(category.id),
                                        onClick = {
                                            pendingCategoryIds = if (pendingCategoryIds.contains(category.id)) {
                                                pendingCategoryIds - category.id
                                            } else {
                                                pendingCategoryIds + category.id
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Apply button
        item {
            val cc = curioColors()
            Button(
                onClick = {
                    viewModel.setSelectedCategoryIds(pendingCategoryIds)
                    onApplyFilter()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cc.secondaryContainer,
                    contentColor = if (CurioApp.darkThemeEnabled) Color(0xFF002021) else Color(0xFFFFFFFF),
                    disabledContainerColor = cc.surfaceContainerHigh.copy(alpha = 0.3f),
                    disabledContentColor = cc.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = if (pendingCategoryIds.isNotEmpty()) "Apply Filters" else "Show All",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // "For You" header
        item {
            val cc = curioColors()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("For You", style = MaterialTheme.typography.titleLarge,
                    color = cc.onSurface, fontWeight = FontWeight.Bold)
                Text("${uiState.discoverContent.size} items",
                    style = MaterialTheme.typography.labelSmall,
                    color = cc.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }

        // Content grid — 2x2
        if (uiState.discoverContent.isNotEmpty()) {
            val chunked = uiState.discoverContent.chunked(2)
            items(chunked.size) { rowIndex ->
                val row = chunked[rowIndex]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { content ->
                        ContentCard(
                            content = content, onClick = {
                                onContentClick(content.id)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun CategoryChip(name: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cc = curioColors()
    val chipBg = if (isSelected) Brush.horizontalGradient(
        colors = listOf(cc.accentGradientStart, cc.accentGradientMid)
    ) else androidx.compose.ui.graphics.SolidColor(cc.surfaceContainerHigh.copy(alpha = 0.3f))
    val emoji = getCategoryEmoji(name)
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(chipBg)
            .clickable { onClick() }.padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (emoji.isNotEmpty()) {
                Text(emoji, fontSize = 14.sp, modifier = Modifier.padding(end = 4.dp))
            }
            Text(name, style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) cc.onSecondaryContainer else cc.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun ContentCard(content: Content, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cc = curioColors()
    val cardGradient = Brush.verticalGradient(
        colors = listOf(cc.cardGradientStart, cc.cardGradientEnd)
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardGradient)
            .heightIn(min = 120.dp, max = 180.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 1.dp, end = 1.dp, top = 1.dp, bottom = 1.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(cc.surfaceContainerHigh)
                .padding(12.dp)
        ) {
            Row(modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            cc.accentGradientStart.copy(alpha = 0.2f),
                            cc.accentGradientMid.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(content.categoryName.uppercase(),
                    style = MaterialTheme.typography.labelSmall, color = cc.accentGradientStart,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(content.title, style = MaterialTheme.typography.labelLarge,
                color = cc.onSurface, fontWeight = FontWeight.Bold, maxLines = 3,
                overflow = TextOverflow.Ellipsis)
            if (content.poet.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(content.poet, style = MaterialTheme.typography.labelSmall,
                    color = cc.primary, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(content.body, style = MaterialTheme.typography.bodySmall,
                color = cc.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 3,
                overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("${content.readTimeSecs}s", style = MaterialTheme.typography.labelSmall,
                    color = cc.accentGradientStart, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\u2764\uFE0F", fontSize = 11.sp)
                    Spacer(Modifier.width(3.dp))
                    Text(formatCount(content.likes),
                        style = MaterialTheme.typography.labelSmall, color = cc.onSurfaceVariant)
                }
            }
        }
    }
}

private fun formatCount(count: Int): String {
    return when { count >= 1000 -> "${count / 1000}k"; else -> count.toString() }
}
