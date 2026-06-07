package com.curio.app.ui.screens.feed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.curio.app.ui.screens.discover.DiscoverScreen
import com.curio.app.ui.screens.profile.ProfileScreen
import com.curio.app.ui.screens.vault.VaultScreen
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainer
import com.curio.app.viewmodel.FeedViewModel

data class NavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainTabScreen(
    onNavigateToContent: (Long) -> Unit = {},
    feedViewModel: FeedViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val uiState by feedViewModel.uiState.collectAsState()

    val navItems = remember {
        listOf(
            NavItem("Feed", Icons.Filled.Style, Icons.Outlined.Style),
            NavItem("Discover", Icons.Filled.Explore, Icons.Outlined.Explore),
            NavItem("Vault", Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks),
            NavItem("Profile", Icons.Filled.Person, Icons.Outlined.Person)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        // Tab content
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tabContent",
            modifier = Modifier.weight(1f)
        ) { tab ->
            when (tab) {
                0 -> FeedScreen(viewModel = feedViewModel)
                1 -> DiscoverScreen(
                    onNavigateToContent = onNavigateToContent,
                    categories = uiState.categories
                )
                2 -> VaultScreen()
                3 -> ProfileScreen()
            }
        }

        // Bottom Navigation Bar
        NavigationBar(
            containerColor = SurfaceContainer.copy(alpha = 0.9f),
            tonalElevation = 0.dp
        ) {
            navItems.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Primary,
                        selectedTextColor = Primary,
                        unselectedIconColor = OnSurfaceVariant,
                        unselectedTextColor = OnSurfaceVariant,
                        indicatorColor = Primary.copy(alpha = 0.12f)
                    )
                )
            }
        }
    }
}
