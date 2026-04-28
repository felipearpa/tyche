package com.felipearpa.tyche.gamblerbets

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

fun NavGraphBuilder.gamblerBetsNavView(navController: NavController) {
    composable<GamblerBetsViewRoute> { navBackStackEntry ->
        val route: GamblerBetsViewRoute = navBackStackEntry.toRoute()
        GamblerBetsView(
            poolId = route.poolId,
            gamblerId = route.gamblerId,
            gamblerUsername = route.gamblerUsername,
            onBack = { navController.navigateUp() },
        )
    }
}
