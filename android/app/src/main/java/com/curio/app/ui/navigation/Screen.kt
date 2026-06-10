package com.curio.app.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object L2Selection : Screen("l2_selection/{l1Name}") {
        fun createRoute(l1Name: String) = "l2_selection/$l1Name"
    }
    data object Main : Screen("main")
    data object ContentDetail : Screen("content/{contentId}") {
        fun createRoute(contentId: Long) = "content/$contentId"
    }

    data object Puzzle : Screen("puzzle/{categoryId}/{puzzleType}") {
        fun createRoute(categoryId: Long, puzzleType: String) = "puzzle/$categoryId/$puzzleType"
    }
}
