package com.felipearpa.tyche.poolhome

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.home.HomeRoute
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute

fun NavGraphBuilder.poolHomeNavView(
    navController: NavController,
    initialRoute: Any,
    gamblerId: String,
) {
    poolHomeView(
        changePool = {
            navController.navigate(route = PoolScoreListRoute(gamblerId = gamblerId)) {
                popUpTo(route = PoolHomeViewRoute) { inclusive = true }
            }
        },
        onLogout = {
            navController.navigate(route = HomeRoute) {
                popUpTo(route = initialRoute) { inclusive = true }
            }
        },
    )
}
