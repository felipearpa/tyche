package com.felipearpa.tyche.pool

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.felipearpa.tyche.pool.joiner.PoolJoinerView
import com.felipearpa.tyche.pool.joiner.poolJoinerViewModel
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute

fun NavGraphBuilder.poolJoinerView(
    navController: NavController,
    gamblerId: String,
    initialRoute: Any,
) {
    composable<PoolJoinerRoute>(
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "felipearpa.github.io/tyche/pools/{poolId}/join"
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
                    popUpTo(route = initialRoute) { inclusive = true }
                }
            },
        )
    }
}
