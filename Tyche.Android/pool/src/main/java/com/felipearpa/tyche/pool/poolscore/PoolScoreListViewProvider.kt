package com.felipearpa.tyche.pool.poolscore

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

fun NavGraphBuilder.poolScoreListView(
    settingsView: @Composable () -> Unit,
    onDetailRequested: (String, String) -> Unit,
) {
    composable<PoolScoreListRoute> { navBackStackEntry ->
        val poolScoreListRoute: PoolScoreListRoute = navBackStackEntry.toRoute()
        PoolScoreListView(
            viewModel = poolScoreListViewModel(gamblerId = poolScoreListRoute.gamblerId),
            drawerView = settingsView,
            onPoolClick = onDetailRequested,
        )
    }
}
