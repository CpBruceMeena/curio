package com.curio.app.ui.screens.onboarding

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.curio.app.CurioApp
import com.curio.app.data.model.L1Group
import com.curio.app.data.repository.ContentRepository
import com.curio.app.ui.theme.OnSecondaryContainer
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainerHigh

private fun l1Gradient(name: String): Pair<Color, Color> = when (name) {
    "Facts" -> Color(0xFF00f4fe) to Color(0xFF0A4A4E)
    "Poems" -> Color(0xFFf472b6) to Color(0xFF4A1A2E)
    "Short Stories" -> Color(0xFF06b6d4) to Color(0xFF0A3A44)
    "Puzzles" -> Color(0xFFf97316) to Color(0xFF4A1E0A)
    else -> Color(0xFFA8CEC8) to Color(0xFF1A2E2A)
}

private fun l1Emoji(name: String): String = when (name) {
    "Facts" -> "\uD83D\uDCD6"
    "Poems" -> "\uD83C\uDFB5"
    "Short Stories" -> "\uD83D\uDCC4"
    "Puzzles" -> "\uD83E\uDDE9"
    else -> "\u2728"
}

private fun subCategoryIcon(name: String): String = when (name.lowercase()) {
    "science" -> "\uD83D\uDD2C"
    "space" -> "\uD83D\uDE80"
    "history" -> "\uD83C\uDFDB"
    "nature" -> "\uD83C\uDF31"
    "technology" -> "\uD83D\uDCBB"
    "animals" -> "\uD83D\uDC3E"
    "geography" -> "\uD83C\uDF0D"
    "mathematics" -> "\u2797"
    "physics" -> "\u26A1"
    "chemistry" -> "\u2697"
    "biology" -> "\uD83E\uDDEB"
    "philosophy" -> "\uD83E\uDD14"
    "psychology" -> "\uD83E\uDDD8"
    "sports" -> "\u26BD"
    "food" -> "\uD83C\uDF54"
    "music" -> "\uD83C\uDFB5"
    "art" -> "\uD83C\uDFA8"
    "poetry" -> "\uD83D\uDCDD"
    "culture" -> "\uD83C\uDF0F"
    "health" -> "\u2764\uFE0F"
    "fitness" -> "\uD83C\uDFCB"
    "business" -> "\uD83D\uDCCA"
    "politics" -> "\uD83C\uDFDB"
    "language" -> "\uD83C\uDF10"
    "literature" -> "\uD83D\uDCDA"
    "programming" -> "\u2328\uFE0F"
    "mythology" -> "\uD83D\uDC7D"
    "ocean" -> "\uD83C\uDF0A"
    "climate" -> "\uD83C\uDF26"
    "engineering" -> "\uD83D\uDEE0"
    "medicine" -> "\uD83D\uDC89"
    "archaeology" -> "\uD83D\uDDFC"
    "economics" -> "\uD83D\uDCB1"
    "astronomy" -> "\uD83D\uDF0C"
    "geology" -> "\uD83E\uDEA8"
    "robotics" -> "\uD83E\uDD16"
    "ai" -> "\uD83E\uDD16"
    else -> "\uD83D\uDCCA"
}

@Composable
fun L2SelectionScreen(
    l1Name: String,
    onNavigateToMain: () -> Unit
) {
    val repository = remember { ContentRepository() }
    var l1Group by remember { mutableStateOf<L1Group?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }

    LaunchedEffect(l1Name) {
        repository.getL1Categories().onSuccess { response ->
            l1Group = response.groups.find { it.name == l1Name }
            isLoading = false
        }.onFailure {
            isLoading = false
        }
    }

    val subcategories = l1Group?.categories ?: emptyList()
    val gradient = l1Gradient(l1Name)
    val emoji = l1Emoji(l1Name)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .statusBarsPadding()
            .padding(bottom = 32.dp)
    ) {
        // Header section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = l1Name,
                style = MaterialTheme.typography.displaySmall,
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pick the topics you'd like to explore",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading topics...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                subcategories.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { cat ->
                            val isSelected = selectedIds.contains(cat.id)
                            L2SubCategoryCard(
                                name = cat.name,
                                emoji = subCategoryIcon(cat.name),
                                isSelected = isSelected,
                                gradient = gradient,
                                onClick = {
                                    selectedIds = if (isSelected) {
                                        selectedIds - cat.id
                                    } else {
                                        selectedIds + cat.id
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size < 3) Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Bottom CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    val categoryNames = if (selectedIds.isNotEmpty()) {
                        subcategories.filter { it.id in selectedIds }.map { it.name }.toSet()
                    } else {
                        subcategories.map { it.name }.toSet()
                    }
                    val app = CurioApp.instance
                    app.prefs.selectedCategories = categoryNames
                    onNavigateToMain()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryContainer,
                    contentColor = Color(0xFF002021)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    text = if (selectedIds.isNotEmpty()) "Explore Selected Topics"
                    else "Explore All $l1Name Topics",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (selectedIds.isNotEmpty()) "${selectedIds.size} topic${if (selectedIds.size != 1) "s" else ""} selected"
                else "Tap topics above to customize",
                style = MaterialTheme.typography.labelMedium,
                color = if (selectedIds.isNotEmpty()) SecondaryContainer else OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun L2SubCategoryCard(
    name: String,
    emoji: String,
    isSelected: Boolean,
    gradient: Pair<Color, Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgModifier = if (isSelected) {
        Modifier.background(
            Brush.linearGradient(
                listOf(gradient.first.copy(alpha = 0.35f), gradient.second.copy(alpha = 0.50f))
            )
        )
    } else {
        Modifier.background(SurfaceContainerHigh.copy(alpha = 0.4f))
    }

    Box(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(bgModifier)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = emoji, fontSize = 18.sp)
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = if (isSelected) OnSurface else OnSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SecondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "✓",
                        color = Color(0xFF002021),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
