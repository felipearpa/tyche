package com.felipearpa.tyche.pool

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.felipearpa.tyche.core.JoinPoolUrlTemplateProvider
import com.felipearpa.tyche.pool.joiner.PoolJoinerView
import com.felipearpa.tyche.pool.joiner.poolJoinerViewModel
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute
import com.felipearpa.tyche.poolscore.POOL_CREATED_KEY

fun NavGraphBuilder.poolJoinerView(
    navController: NavController,
    gamblerId: String,
    initialRoute: Any,
    joinPoolUrlTemplate: JoinPoolUrlTemplateProvider,
) {
    composable<PoolJoinerRoute>(
        deepLinks = listOf(
            navDeepLink {
                uriPattern = String.format(joinPoolUrlTemplate(), "{poolId}")
            },
        ),
    ) {
        val route = it.toRoute<PoolJoinerRoute>()
        PoolJoinerView(
            viewModel = poolJoinerViewModel(),
            poolId = route.poolId,
            gamblerId = gamblerId,
            onJoinPool = {
                navController.navigate(route = PoolScoreListRoute(gamblerId = gamblerId)) {
                    runCatching {
                        navController.getBackStackEntry<PoolScoreListRoute>()
                            .savedStateHandle[POOL_CREATED_KEY] = true
                    }

                    popUpTo(route = initialRoute) { inclusive = true }
                }
            },
            onAbort = {
                navController.navigate(route = PoolScoreListRoute(gamblerId = gamblerId)) {
                    popUpTo(route = initialRoute) { inclusive = true }
                }
            },
        )
    }
}
