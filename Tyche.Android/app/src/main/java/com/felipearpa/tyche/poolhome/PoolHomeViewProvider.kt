package com.felipearpa.tyche.poolhome

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

fun NavGraphBuilder.poolHomeView(onPoolScoreListRequested: () -> Unit, onLogout: () -> Unit) {
    composable(
        route = PoolHomeViewRoute.route,
        arguments = listOf(
            navArgument(name = PoolHomeViewRoute.Param.GAMBLER_ID.id) {
                type = NavType.StringType
            },
            navArgument(name = PoolHomeViewRoute.Param.POOL_ID.id) {
                type = NavType.StringType
            }
        )
    ) { navBackStackEntry ->
        val poolId =
            navBackStackEntry.arguments!!.getString(PoolHomeViewRoute.Param.POOL_ID.id)!!
        val gamblerId =
            navBackStackEntry.arguments!!.getString(PoolHomeViewRoute.Param.GAMBLER_ID.id)!!
        PoolHomeView(
            viewModel = poolHomeViewModel(poolId = poolId, gamblerId = gamblerId),
            onPoolScoreListRequested = onPoolScoreListRequested,
            onLogout = onLogout
        )
    }
}