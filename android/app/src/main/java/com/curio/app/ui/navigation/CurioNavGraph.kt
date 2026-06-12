package com.curio.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.curio.app.ui.screens.content.ContentDetailScreen
import com.curio.app.ui.screens.feed.MainTabScreen
import com.curio.app.ui.screens.novel.NovelDetailScreen
import com.curio.app.ui.screens.novel.NovelReaderScreen
import com.curio.app.ui.screens.novel.NovelsFeedScreen
import com.curio.app.ui.screens.onboarding.L2SelectionScreen
import com.curio.app.ui.screens.onboarding.OnboardingScreen
import com.curio.app.ui.screens.puzzle.PuzzleScreen
import com.curio.app.ui.screens.splash.SplashScreen

@Composable
fun CurioNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route)
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToFeed = {
                    navController.navigate(Screen.Main.route)
                },
                onNavigateToL2 = { l1Name ->
                    navController.navigate(Screen.L2Selection.createRoute(l1Name))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.L2Selection.route,
            arguments = listOf(navArgument("l1Name") { type = NavType.StringType })
        ) { backStackEntry ->
            val l1Name = backStackEntry.arguments?.getString("l1Name") ?: ""
            L2SelectionScreen(
                l1Name = l1Name,
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Main.route) {
            MainTabScreen(
                onPuzzleNavigate = { categoryId, puzzleType ->
                    navController.navigate(Screen.Puzzle.createRoute(categoryId, puzzleType))
                },
                onContentClick = { contentId ->
                    navController.navigate(Screen.ContentDetail.createRoute(contentId))
                },
                onNovelClick = { novelId ->
                    navController.navigate(Screen.NovelDetail.createRoute(novelId))
                }
            )
        }

        composable(Screen.ContentDetail.route) { backStackEntry ->
            val contentId = backStackEntry.arguments?.getString("contentId")?.toLongOrNull() ?: 0L
            ContentDetailScreen(
                contentId = contentId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Puzzle.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.LongType },
                navArgument("puzzleType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L
            val puzzleType = backStackEntry.arguments?.getString("puzzleType") ?: ""
            PuzzleScreen(
                categoryId = categoryId,
                puzzleType = puzzleType,
                onBack = { navController.popBackStack() },
                onAllDone = { navController.popBackStack() }
            )
        }

        // ── Novels ────────────────────────────────────────────
        composable(Screen.NovelsFeed.route) {
            NovelsFeedScreen(
                onNovelClick = { novelId ->
                    navController.navigate(Screen.NovelDetail.createRoute(novelId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.NovelDetail.route,
            arguments = listOf(navArgument("novelId") { type = NavType.LongType })
        ) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getLong("novelId") ?: 0L
            NovelDetailScreen(
                novelId = novelId,
                onChapterClick = { chapterNum ->
                    navController.navigate(Screen.NovelReader.createRoute(novelId, chapterNum))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.NovelReader.route,
            arguments = listOf(
                navArgument("novelId") { type = NavType.LongType },
                navArgument("chapterNum") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getLong("novelId") ?: 0L
            val chapterNum = backStackEntry.arguments?.getInt("chapterNum") ?: 1
            NovelReaderScreen(
                novelId = novelId,
                initialChapter = chapterNum,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
