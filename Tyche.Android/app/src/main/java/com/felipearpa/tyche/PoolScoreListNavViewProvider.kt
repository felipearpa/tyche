package com.felipearpa.tyche

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.home.ui.HomeRoute
import com.felipearpa.tyche.pool.poolscore.poolScoreListView
import com.felipearpa.tyche.poolhome.PoolHomeViewRoute
import com.felipearpa.tyche.settings.SettingsView
import com.felipearpa.tyche.settings.settingsViewModel

fun NavGraphBuilder.poolScoreListView(
    navController: NavController,
    initialRoute: String,
    loggedInGamblerId: String?
) {
    poolScoreListView(
        loggedInGamblerId = loggedInGamblerId,
        settingsView = {
            SettingsView(
                viewModel = settingsViewModel(),
                onLogout = {
                    navController.navigate(route = HomeRoute.route()) {
                        popUpTo(route = initialRoute) { inclusive = true }
                    }
                }
            )
        },
        onDetailRequested = { poolId, gamblerId ->
            navController.navigate(
                route = PoolHomeViewRoute.route(
                    poolId = poolId,
                    gamblerId = gamblerId
                )
            )
        }
    )
}