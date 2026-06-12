package com.curio.app.ui.screens.journal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.app.ui.theme.curioColors
import com.curio.app.viewmodel.JournalViewModel


/**
 * Internal navigation states for Journal:
 *   0 = entries list, 1 = editor (new/edit)
 *   3 = prompt selector (shown on first open when today has no entries)
 */
private const val VIEW_LIST = 0
private const val VIEW_EDITOR = 1
private const val VIEW_PROMPTS = 3

@Composable
fun JournalScreen(
    viewModel: JournalViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val cc = curioColors()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = viewModel.selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                            (slideOutHorizontally { it / 3 } + fadeOut())
                }
            },
            label = "journal_nav"
        ) { tab ->
            when (tab) {
                VIEW_EDITOR -> {
                    JournalEditorScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.selectedTab = VIEW_LIST }
                    )
                }
                VIEW_PROMPTS -> {
                    JournalPromptSelector(
                        viewModel = viewModel,
                        onDismiss = {
                            viewModel.startNewEntry()
                        }
                    )
                }
                else -> {
                    JournalListScreen(viewModel = viewModel)
                }
            }
        }

        // FAB — only visible on list view
        if (viewModel.selectedTab == VIEW_LIST) {
            FloatingActionButton(
                onClick = { viewModel.startNewEntry() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 16.dp),
                containerColor = cc.accentGradientStart,
                contentColor = Color(0xFF002021),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New entry",
                    tint = Color(0xFF002021),
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}
