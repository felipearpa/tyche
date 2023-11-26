package com.felipearpa.tyche

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.pool.poolscore.poolScoreListView
import com.felipearpa.tyche.poolHome.PoolHomeViewRoute

fun NavGraphBuilder.poolScoreListView(navController: NavController, loggedInGamblerId: String?) {
    poolScoreListView(
        loggedInGamblerId = loggedInGamblerId,
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