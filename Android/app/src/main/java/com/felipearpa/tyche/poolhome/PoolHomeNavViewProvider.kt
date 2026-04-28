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
) {
    composable<PoolHomeViewRoute> { navBackStackEntry ->
        val route: PoolHomeViewRoute = navBackStackEntry.toRoute()
        PoolHomeView(
            poolId = route.poolId,
            gamblerId = route.gamblerId,
            loggedInGamblerId = route.loggedInGamblerId,
            onPoolChange = {
                navController.navigate(route = PoolScoreListRoute(gamblerId = route.loggedInGamblerId)) {
                    popUpTo<PoolHomeViewRoute> { inclusive = true }
                }
            },
            onSignOut = {
                navController.navigate(route = HomeRoute) {
                    popUpTo(route = initialRoute) { inclusive = true }
                }
            },
            onGamblerOpen = { _, tappedGamblerId ->
                if (tappedGamblerId != route.gamblerId) {
                    navController.navigate(
                        route = PoolHomeViewRoute(
                            poolId = route.poolId,
                            gamblerId = tappedGamblerId,
                            loggedInGamblerId = route.loggedInGamblerId,
                        ),
                    )
                }
            },
        )
    }
}
