package com.curio.app.ui.screens.feed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.app.CurioApp
import com.curio.app.ui.components.FeedbackDialog
import com.curio.app.ui.screens.discover.DiscoverScreen
import com.curio.app.ui.screens.journal.JournalScreen
import com.curio.app.ui.screens.profile.ProfileScreen
import com.curio.app.ui.theme.curioColors
import com.curio.app.viewmodel.FeedViewModel

private enum class BottomTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Discover("Discover", Icons.Filled.Explore, Icons.Outlined.Explore),
    Feed("Feed", Icons.Filled.Home, Icons.Outlined.Home),
    Bookmarks("Bookmarks", Icons.Filled.Bookmark, Icons.Outlined.Bookmark),
    Journal("Journal", Icons.Filled.EditNote, Icons.Outlined.EditNote),
    Profile("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun MainTabScreen(
    feedViewModel: FeedViewModel = viewModel(),
    onPuzzleNavigate: (categoryId: Long, puzzleType: String) -> Unit = { _, _ -> },
    onContentClick: (Long) -> Unit = {}
) {
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var currentCategory by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(BottomTab.Feed) }
    var startBookmarkPagerIndex by remember { mutableStateOf<Int?>(null) }

    val prefs = remember { CurioApp.instance.prefs }

    val isFeed = selectedTab == BottomTab.Feed
    val isDiscover = selectedTab == BottomTab.Discover
    val isBookmarks = selectedTab == BottomTab.Bookmarks
    val isJournal = selectedTab == BottomTab.Journal
    val isProfile = selectedTab == BottomTab.Profile

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        // ── Compact Top Bar ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            val cc = curioColors()
            when {
                isFeed -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentCategory,
                                style = MaterialTheme.typography.titleMedium,
                                color = cc.secondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            // Auto-play toggle button
                            IconButton(
                                onClick = { feedViewModel.toggleAutoPlay() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (feedViewModel.autoPlayEnabled) Icons.Filled.PauseCircle
                                        else Icons.Filled.PlayCircle,
                                    contentDescription = if (feedViewModel.autoPlayEnabled) "Stop auto-play" else "Auto-play all",
                                    tint = if (feedViewModel.autoPlayEnabled) cc.accentGradientStart
                                        else cc.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Shuffle button
                        IconButton(
                            onClick = { feedViewModel.shuffleFeed() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Shuffle",
                                tint = cc.accentGradientStart.copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                isDiscover -> {
                    Text(
                        text = "Discover",
                        style = MaterialTheme.typography.titleMedium,
                        color = cc.secondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                isBookmarks -> {
                    Text(
                        text = "Bookmarks",
                        style = MaterialTheme.typography.titleMedium,
                        color = cc.bookmarkActive,
                        fontWeight = FontWeight.Bold
                    )
                }
                isJournal -> {
                    Text(
                        text = "Journal",
                        style = MaterialTheme.typography.titleMedium,
                        color = cc.accentGradientStart,
                        fontWeight = FontWeight.Bold
                    )
                }
                isProfile -> {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleMedium,
                        color = cc.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── Main Content Area ──
        Box(modifier = Modifier.weight(1f)) {
            val bookmarkIndex = startBookmarkPagerIndex
            when {
                bookmarkIndex != null -> {
                    BookmarkPagerScreen(
                        viewModel = feedViewModel,
                        startIndex = bookmarkIndex,
                        onBack = { startBookmarkPagerIndex = null }
                    )
                }
                selectedTab == BottomTab.Feed -> {
                    FeedScreen(
                        viewModel = feedViewModel,
                        onCategoryChange = { category -> currentCategory = category },
                        onNavigateToDiscover = { selectedTab = BottomTab.Discover }
                    )
                }
                selectedTab == BottomTab.Discover -> {
                    DiscoverScreen(
                        viewModel = feedViewModel,
                        onApplyFilter = { selectedTab = BottomTab.Feed },
                        onCategoryClick = { categoryId ->
                            feedViewModel.setSelectedCategoryIds(setOf(categoryId))
                            selectedTab = BottomTab.Feed
                        },
                        onPuzzleNavigate = { categoryId, puzzleType ->
                            onPuzzleNavigate(categoryId, puzzleType)
                        },
                        onContentClick = onContentClick
                    )
                }
                selectedTab == BottomTab.Bookmarks -> {
                    BookmarksScreen(
                        viewModel = feedViewModel,
                        onContentClick = { contentId ->
                            val bookmarked = feedViewModel.uiState.value.bookmarkedContent
                            val index = bookmarked.indexOfFirst { it.id == contentId }
                            if (index >= 0) {
                                startBookmarkPagerIndex = index
                            }
                        }
                    )
                }
                selectedTab == BottomTab.Journal -> {
                    JournalScreen()
                }
                selectedTab == BottomTab.Profile -> {
                    ProfileScreen(prefs = prefs)
                }
            }
        }

        // ── Bottom Navigation Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTab.entries.forEach { tab ->
                val cc = curioColors()
                val isActive = selectedTab == tab
                val tabColor = if (isActive) {
                    when (tab) {
                        BottomTab.Discover -> cc.secondaryContainer
                        BottomTab.Feed -> cc.secondaryContainer
                        BottomTab.Bookmarks -> cc.bookmarkActive
                        BottomTab.Journal -> cc.accentGradientStart
                        BottomTab.Profile -> cc.onSurface
                    }
                } else {
                    cc.onSurfaceVariant.copy(alpha = 0.5f)
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (tab == BottomTab.Discover) feedViewModel.refreshDiscover()
                            if (tab == BottomTab.Bookmarks) feedViewModel.loadBookmarkedContent()
                            selectedTab = tab
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isActive) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        tint = tabColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tabColor,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // ── Feedback FAB (small clickable text above bottom bar) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            val cc = curioColors()
            Text(
                text = "💬 Feedback",
                fontSize = 11.sp,
                color = cc.onSurfaceVariant.copy(alpha = 0.4f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showFeedbackDialog = true }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
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
