package com.curio.app.ui.screens.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.app.ui.components.InterestChip
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainer
import com.curio.app.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    onNavigateToFeed: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                .padding(bottom = 32.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Curio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Step 2 of 3",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant
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
                    text = "Select at least one interest to personalize your feed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }

            // Interest grid
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading interests...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.categories) { category ->
                        InterestChip(
                            name = category.name,
                            icon = category.icon,
                            isSelected = uiState.selectedInterests.contains(category.name),
                            onClick = { viewModel.toggleInterest(category.name) }
                        )
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
                        onNavigateToFeed()
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
                        text = "Start Learning",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(                        text = if (uiState.canProceed) {
                                "Great! ${uiState.selectedInterests.size} interest${if (uiState.selectedInterests.size > 1) "s" else ""} selected"
                            } else {
                                "Select at least one interest to continue"
                            },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (uiState.canProceed) SecondaryContainer else OnSurfaceVariant
                )
            }
        }
    }
}
