package com.felipearpa.tyche.pool.poolscore

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

fun NavGraphBuilder.poolScoreListView(
    loggedInGamblerId: String?,
    settingsView: @Composable () -> Unit,
    onDetailRequested: (String, String) -> Unit
) {
    composable(
        route = PoolScoreListRoute.route,
        arguments = listOf(
            navArgument(name = PoolScoreListRoute.Param.GAMBLER_ID.id) {
                type = NavType.StringType

                loggedInGamblerId?.let {
                    defaultValue = loggedInGamblerId
                }
            }
        )
    ) { navBackStackEntry ->
        val gamblerId =
            navBackStackEntry.arguments?.getString(PoolScoreListRoute.Param.GAMBLER_ID.id)!!
        PoolScoreListView(
            viewModel = poolScoreListViewModel(gamblerId = gamblerId),
            drawerView = settingsView,
            onDetailRequested = onDetailRequested
        )
    }
}