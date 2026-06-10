package com.curio.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.curio.app.ui.screens.content.ContentDetailScreen
import com.curio.app.ui.screens.feed.MainTabScreen
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
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToFeed = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToL2 = { l1Name ->
                    navController.navigate(Screen.L2Selection.createRoute(l1Name)) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
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
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.L2Selection.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainTabScreen(
                onPuzzleNavigate = { categoryId, puzzleType ->
                    navController.navigate(Screen.Puzzle.createRoute(categoryId, puzzleType))
                },
                onContentClick = { contentId ->
                    navController.navigate(Screen.ContentDetail.createRoute(contentId))
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
    }
}
