package com.curio.app.ui.screens.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainer
import com.curio.app.viewmodel.OnboardingViewModel

private fun l1Emoji(name: String): String = when (name) {
    "Facts" -> "\uD83D\uDCD6"
    "Poems" -> "\uD83C\uDFB5"
    "Short Stories" -> "\uD83D\uDCC4"
    "Puzzles" -> "\uD83E\uDDE9"
    "Novels" -> "\uD83D\uDCDA"
    else -> "\u2728"
}

private fun l1Gradient(name: String): Pair<Color, Color> = when (name) {
    "Facts" -> Color(0xFF00f4fe) to Color(0xFF0A4A4E)
    "Poems" -> Color(0xFFf472b6) to Color(0xFF4A1A2E)
    "Short Stories" -> Color(0xFF06b6d4) to Color(0xFF0A3A44)
    "Puzzles" -> Color(0xFFf97316) to Color(0xFF4A1E0A)
    "Novels" -> Color(0xFF8b5cf6) to Color(0xFF2A1A4E)
    else -> Color(0xFFA8CEC8) to Color(0xFF1A2E2A)
}

@Composable
fun OnboardingScreen(
    onNavigateToFeed: () -> Unit,
    onNavigateToL2: (String) -> Unit,
    onBack: () -> Unit = {},
    viewModel: OnboardingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val sortedGroups = uiState.l1Groups.sortedBy { g ->
        when (g.name) { "Facts" -> 0; "Poems" -> 1; "Short Stories" -> 2; "Puzzles" -> 3; "Novels" -> 4; else -> 5 }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0x22103632),
                        Surface
                    ),
                    radius = 1000f
                )
            )
    ) {
        // Decorative glow
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 20.dp)
                .size(200.dp)
                .blur(80.dp)
                .background(Primary.copy(alpha = 0.06f), RoundedCornerShape(100.dp))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = OnSurfaceVariant
                    )
                }
                Text(
                    text = "Curio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Primary,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "1 / 3",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }

            // Header
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "What piques your curiosity?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pick what you'd like to discover. You can always change later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // L1 Cards (2x2 grid) — show immediately, no loading blocker
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sortedGroups.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { group ->
                            val isSelected = uiState.selectedInterest == group.name
                            OnboardingL1Card(
                                name = group.name,
                                emoji = l1Emoji(group.name),
                                gradient = l1Gradient(group.name),
                                subCount = group.categories.size,
                                isSelected = isSelected,
                                isRefreshing = uiState.isRefreshing,
                                onClick = { viewModel.toggleInterest(group.name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
                    }
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
                        viewModel.saveInterests()
                        val subCats = viewModel.getSelectedL1Subcategories()
                        if (subCats.size > 1) {
                            onNavigateToL2(uiState.selectedInterest!!)
                        } else {
                            // Save all subcategories and go to Main
                            val names = subCats.map { it.name }.toSet()
                            viewModel.saveFinalSelection(names)
                            onNavigateToFeed()
                        }
                    },
                    enabled = uiState.canProceed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryContainer,
                        contentColor = Color(0xFF002021),
                        disabledContainerColor = SurfaceContainer,
                        disabledContentColor = OnSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (uiState.canProceed) 8.dp else 0.dp
                    )
                ) {
                    Text(
                        text = if (uiState.selectedInterest != null) {
                            "Explore ${uiState.selectedInterest}"
                        } else {
                            "Start Learning"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // No subtitle text below button — user explicitly removed this
            }
        }
    }
}

@Composable
private fun OnboardingL1Card(
    name: String, emoji: String, gradient: Pair<Color, Color>,
    subCount: Int, isSelected: Boolean, isRefreshing: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    // ── Shimmer animation ──
    // Sweeping highlight effect while API refreshes data
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )

    // Fade out shimmer alpha smoothly when refresh completes
    val shimmerAlpha by animateFloatAsState(
        targetValue = if (isRefreshing) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "shimmerAlpha"
    )

    Box(
        modifier = modifier
            .height(170.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) {
                    Brush.linearGradient(listOf(gradient.first.copy(alpha = 0.4f), gradient.second.copy(alpha = 0.6f)))
                } else {
                    Brush.linearGradient(listOf(gradient.first.copy(alpha = 0.18f), gradient.second.copy(alpha = 0.25f)))
                }
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        // ── Shimmer overlay ──
        // Subtle diagonal light sweep across the card, only visible while refreshing
        if (shimmerAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0f),
                                Color.White.copy(alpha = 0.06f * shimmerAlpha),
                                Color.White.copy(alpha = 0f),
                            ),
                            start = Offset(shimmerProgress * 500f, 0f),
                            end = Offset(shimmerProgress * 500f + 300f, 500f)
                        )
                    )
            )
        }

        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(emoji, fontSize = 40.sp)
                // Selection indicator
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SecondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\u2713", color = Color(0xFF002021),
                            fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
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
