package com.felipearpa.tyche.poolhome

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

fun NavGraphBuilder.poolHomeView(changePool: () -> Unit, onLogout: () -> Unit) {
    composable<PoolHomeViewRoute> { navBackStackEntry ->
        val route: PoolHomeViewRoute = navBackStackEntry.toRoute()
        PoolHomeView(
            poolId = route.poolId,
            gamblerId = route.gamblerId,
            changePool = changePool,
            onLogout = onLogout,
        )
    }
}
