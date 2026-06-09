package com.curio.app.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.model.Content
import com.curio.app.ui.theme.OnSecondaryContainer
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainerHigh
import com.curio.app.viewmodel.FeedViewModel

@Composable
fun DiscoverScreen(
    viewModel: FeedViewModel,
    onApplyFilter: () -> Unit = {},
    onCategoryClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingCategoryIds by remember { mutableStateOf(uiState.selectedCategoryIds) }
    // Selected L1 group — shows its subcategories when non-null
    var selectedL1 by remember { mutableStateOf<String?>(null) }

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
        modifier = Modifier.fillMaxSize().background(Surface),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Discover", style = MaterialTheme.typography.headlineLarge,
                    color = OnSurface, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Explore curated knowledge across every topic.",
                    style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            }
        }

        // L1 Category pills
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val l1Groups = uiState.l1Groups
                l1Groups.forEach { group ->
                    val isActive = selectedL1 == group.name
                    Box(modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) SecondaryContainer else SurfaceContainerHigh.copy(alpha = 0.3f))
                        .clickable {
                            selectedL1 = if (isActive) null else group.name
                            pendingCategoryIds = emptySet()
                        }
                        .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isActive) OnSecondaryContainer else OnSurfaceVariant,
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedL1 != null) "$selectedL1 Topics" else "All Categories",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface, fontWeight = FontWeight.Bold
                )
                if (pendingCategoryIds.isNotEmpty()) {
                    Text("Clear", style = MaterialTheme.typography.labelMedium,
                        color = Primary, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { pendingCategoryIds = emptySet() })
                }
            }
        }

        // Category chips (filtered by L1)
        if (displayCategories.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayCategories.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { category ->
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
                            repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Apply button
        item {
            Button(
                onClick = {
                    viewModel.setSelectedCategoryIds(pendingCategoryIds)
                    onApplyFilter()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryContainer,
                    contentColor = Color(0xFF002021),
                    disabledContainerColor = SurfaceContainerHigh.copy(alpha = 0.3f),
                    disabledContentColor = OnSurfaceVariant.copy(alpha = 0.5f)
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("For You", style = MaterialTheme.typography.titleLarge,
                    color = OnSurface, fontWeight = FontWeight.Bold)
                Text("${uiState.discoverContent.size} items",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.6f))
            }
        }

        // Content grid
        if (uiState.discoverContent.isNotEmpty()) {
            val chunked = uiState.discoverContent.chunked(2)
            items(chunked.size) { rowIndex ->
                val row = chunked[rowIndex]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { content ->
                        ContentCard(
                            content = content, onClick = {
                                onCategoryClick(content.categoryId)
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight()
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
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SecondaryContainer else SurfaceContainerHigh.copy(alpha = 0.3f))
            .clickable { onClick() }.padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(name, style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) OnSecondaryContainer else OnSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun ContentCard(content: Content, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerHigh).heightIn(min = 180.dp, max = 300.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight()
                .verticalScroll(rememberScrollState()).padding(14.dp)
        ) {
            Row(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                .background(SecondaryContainer.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(content.categoryName.uppercase(),
                    style = MaterialTheme.typography.labelSmall, color = SecondaryContainer,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(content.title, style = MaterialTheme.typography.labelLarge,
                color = OnSurface, fontWeight = FontWeight.Bold, maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if (content.poet.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(content.poet, style = MaterialTheme.typography.labelSmall,
                    color = Primary, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(content.body, style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant.copy(alpha = 0.7f), maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if (content.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(SecondaryContainer.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text(content.description, style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant.copy(alpha = 0.7f), maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("${content.readTimeSecs}s", style = MaterialTheme.typography.labelSmall,
                    color = Primary, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\u2764\uFE0F", fontSize = 11.sp)
                    Spacer(Modifier.width(3.dp))
                    Text(formatCount(content.likes),
                        style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                }
            }
        }
    }
}

private fun formatCount(count: Int): String {
    return when { count >= 1000 -> "${count / 1000}k"; else -> count.toString() }
}
