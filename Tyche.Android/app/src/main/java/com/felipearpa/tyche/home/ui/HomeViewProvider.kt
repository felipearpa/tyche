package com.felipearpa.tyche.home.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.homeView(onLoginRequested: () -> Unit, onNewAccountRequested: () -> Unit) {
    composable(route = HomeRoute.route) {
        HomeView(
            viewModel = homeViewModel(),
            onLoginRequested = onLoginRequested,
            onNewAccountRequested = onNewAccountRequested
        )
    }
}