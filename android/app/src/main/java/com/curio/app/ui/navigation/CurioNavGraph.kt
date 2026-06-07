package com.curio.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.curio.app.ui.screens.feed.MainTabScreen
import com.curio.app.ui.screens.onboarding.OnboardingScreen
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
                }
            )
        }

        composable(Screen.Main.route) {
            MainTabScreen(
                onNavigateToContent = { contentId ->
                    navController.navigate(Screen.ContentDetail.createRoute(contentId))
                }
            )
        }

        composable(Screen.ContentDetail.route) { backStackEntry ->
            val contentId = backStackEntry.arguments?.getString("contentId")?.toLongOrNull() ?: 0L
            // Content detail screen placeholder (Phase 2)
        }
    }
}
