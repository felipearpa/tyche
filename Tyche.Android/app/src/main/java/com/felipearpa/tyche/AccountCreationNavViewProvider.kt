package com.felipearpa.tyche

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.account.managment.accountCreationView
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute

fun NavGraphBuilder.accountCreationView(navController: NavController, initialRoute: String) {
    accountCreationView(
        onAccountCreated = { createdUserProfile ->
            navController.navigate(route = PoolScoreListRoute.route(gamblerId = createdUserProfile.userId)) {
                popUpTo(route = initialRoute) { inclusive = true }
            }
        },
        onBackRequested = { navController.navigateUp() }
    )
}