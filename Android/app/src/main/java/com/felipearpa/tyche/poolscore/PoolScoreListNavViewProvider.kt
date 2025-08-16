package com.felipearpa.tyche.poolscore

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.felipearpa.tyche.home.HomeRoute
import com.felipearpa.tyche.pool.creator.PoolFromLayoutCreatorRoute
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute
import com.felipearpa.tyche.pool.poolscore.PoolScoreListView
import com.felipearpa.tyche.pool.poolscore.poolScoreListViewModel
import com.felipearpa.tyche.poolhome.PoolHomeViewRoute
import com.felipearpa.tyche.poolscore.drawer.DrawerView
import com.felipearpa.tyche.poolscore.drawer.drawerViewModel

fun NavGraphBuilder.poolScoreListNavView(
    navController: NavController,
    initialRoute: Any,
) {
    composable<PoolScoreListRoute> { navBackStackEntry ->
        val poolScoreListRoute: PoolScoreListRoute = navBackStackEntry.toRoute()
        PoolScoreListView(
            viewModel = poolScoreListViewModel(gamblerId = poolScoreListRoute.gamblerId),
            drawerView = {
                DrawerView(
                    viewModel = drawerViewModel(),
                    onLogout = {
                        navController.navigate(route = HomeRoute) {
                            popUpTo(route = initialRoute) { inclusive = true }
                        }
                    },
                )
            },
            onPoolOpen = { poolId, gamblerId ->
                navController.navigate(
                    route = PoolHomeViewRoute(
                        poolId = poolId,
                        gamblerId = gamblerId,
                    ),
                ) { popUpTo(route = initialRoute) { inclusive = true } }
            },
            onPoolCreate = { navController.navigate(route = PoolFromLayoutCreatorRoute) },
        )
    }
}
