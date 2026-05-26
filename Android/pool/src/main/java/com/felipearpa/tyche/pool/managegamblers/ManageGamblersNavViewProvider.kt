package com.felipearpa.tyche.pool.managegamblers

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

fun NavGraphBuilder.manageGamblersNavView(navController: NavController) {
    composable<ManageGamblersRoute> { navBackStackEntry ->
        val route: ManageGamblersRoute = navBackStackEntry.toRoute()
        ManageGamblersView(
            poolId = route.poolId,
            gamblerId = route.gamblerId,
            onBack = { navController.navigateUp() },
        )
    }
}
