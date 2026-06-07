package com.curio.app.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Main : Screen("main")
    data object ContentDetail : Screen("content/{contentId}") {
        fun createRoute(contentId: Long) = "content/$contentId"
    }
}
