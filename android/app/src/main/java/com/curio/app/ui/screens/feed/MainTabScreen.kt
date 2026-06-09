package com.curio.app.ui.screens.feed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.app.ui.components.FeedbackDialog
import com.curio.app.ui.screens.discover.DiscoverScreen
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Surface
import com.curio.app.viewmodel.FeedViewModel

@Composable
fun MainTabScreen(
    feedViewModel: FeedViewModel = viewModel()
) {
    var showDiscover by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .systemBarsPadding()
    ) {
        // Top bar with discover toggle icon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(
                onClick = { showDiscover = !showDiscover }
            ) {
                Icon(
                    imageVector = if (showDiscover) Icons.AutoMirrored.Filled.ArrowBack
                                   else Icons.Filled.Explore,
                    contentDescription = if (showDiscover) "Back to feed" else "Discover categories",
                    tint = OnSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Main content — feed by default, discover on icon tap
        AnimatedContent(
            targetState = showDiscover,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "mainContent",
            modifier = Modifier.weight(1f)
        ) { isDiscover ->
            if (isDiscover) {
                DiscoverScreen(
                    viewModel = feedViewModel,
                    onApplyFilter = {
                        showDiscover = false
                    },
                    onCategoryClick = { categoryId ->
                        feedViewModel.setSelectedCategoryIds(setOf(categoryId))
                        showDiscover = false
                    }
                )
            } else {
                FeedScreen(viewModel = feedViewModel)
            }
        }

        // Bottom feedback bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "💬  Send Feedback",
                fontSize = 13.sp,
                color = OnSurfaceVariant.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showFeedbackDialog = true }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }

    // Feedback dialog
    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismiss = { showFeedbackDialog = false }
        )
    }
}
