package com.felipearpa.tyche.poolhome

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.felipearpa.tyche.home.HomeRoute
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute

fun NavGraphBuilder.poolHomeNavView(
    navController: NavController,
    initialRoute: Any,
    gamblerId: String,
) {
    composable<PoolHomeViewRoute> { navBackStackEntry ->
        val route: PoolHomeViewRoute = navBackStackEntry.toRoute()
        PoolHomeView(
            poolId = route.poolId,
            gamblerId = route.gamblerId,
            onPoolChange = {
                navController.navigate(route = PoolScoreListRoute(gamblerId = gamblerId)) {
                    popUpTo(route = PoolHomeViewRoute) { inclusive = true }
                }
            },
            onSignOut = {
                navController.navigate(route = HomeRoute) {
                    popUpTo(route = initialRoute) { inclusive = true }
                }
            },
        )
    }
}
