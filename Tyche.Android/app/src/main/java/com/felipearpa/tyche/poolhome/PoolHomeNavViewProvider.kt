package com.felipearpa.tyche.poolhome

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.home.ui.HomeRoute
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute
import com.felipearpa.tyche.poolhome.PoolHomeViewRoute
import com.felipearpa.tyche.poolhome.poolHomeView

fun NavGraphBuilder.poolHomeNavView(
    navController: NavController,
    initialRoute: String,
    gamblerId: String
) {
    poolHomeView(
        changePool = {
            navController.navigate(route = PoolScoreListRoute.route(gamblerId = gamblerId)) {
                popUpTo(route = PoolHomeViewRoute.route) { inclusive = true }
            }
        },
        onLogout = {
            navController.navigate(route = HomeRoute.route()) {
                popUpTo(route = initialRoute) { inclusive = true }
            }
        }
    )
}