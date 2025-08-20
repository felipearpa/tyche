package com.felipearpa.tyche.poolcreator

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.pool.creator.poolFromLayoutCreatorView
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute
import com.felipearpa.tyche.poolscore.POOL_CREATED_KEY

fun NavGraphBuilder.poolFromLayoutCreatorNavView(navController: NavController, gamblerId: String) {
    poolFromLayoutCreatorView(
        gamblerId = gamblerId,
        onPoolCreated = {
            runCatching {
                navController.getBackStackEntry<PoolScoreListRoute>()
                    .savedStateHandle[POOL_CREATED_KEY] = true
            }

            navController.popBackStack()
        },
        onBackClick = { navController.popBackStack() },
    )
}
