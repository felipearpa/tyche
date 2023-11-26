package com.felipearpa.tyche

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.account.login.loginView
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute

fun NavGraphBuilder.loginView(navController: NavController, initialRoute: String) {
    loginView(
        onLoggedId = { accountBundle ->
            navController.navigate(route = PoolScoreListRoute.route(gamblerId = accountBundle.userId)) {
                popUpTo(route = initialRoute) { inclusive = true }
            }
        },
        onBackRequested = { navController.navigateUp() }
    )
}