package com.curio.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.curio.app.ui.screens.feed.FeedScreen
import com.curio.app.ui.theme.Surface
import com.curio.app.viewmodel.FeedViewModel

@Composable
fun HomeScreen(viewModel: FeedViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        FeedScreen(viewModel = viewModel)
    }
}
