package com.felipearpa.tyche.poolscore

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.home.ui.HomeRoute
import com.felipearpa.tyche.pool.creator.PoolFromLayoutCreatorRoute
import com.felipearpa.tyche.pool.poolscore.poolScoreListView
import com.felipearpa.tyche.poolhome.PoolHomeViewRoute
import com.felipearpa.tyche.poolscore.drawer.DrawerView
import com.felipearpa.tyche.poolscore.drawer.drawerViewModel

fun NavGraphBuilder.poolScoreListNavView(
    navController: NavController,
    initialRoute: Any,
) {
    poolScoreListView(
        settingsView = {
            DrawerView(
                viewModel = drawerViewModel(),
                onLogout = {
                    navController.navigate(route = HomeRoute) {
                        popUpTo(route = initialRoute) { inclusive = true }
                    }
                },
            )
        },
        onDetailClick = { poolId, gamblerId ->
            navController.navigate(
                route = PoolHomeViewRoute(
                    poolId = poolId,
                    gamblerId = gamblerId,
                ),
            ) { popUpTo(route = initialRoute) { inclusive = true } }
        },
        onCreatePoolClick = {
            navController.navigate(route = PoolFromLayoutCreatorRoute)
        },
    )
}
