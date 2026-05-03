package com.felipearpa.tyche.poolhome

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.felipearpa.tyche.bet.match.MatchBetListViewRoute
import com.felipearpa.tyche.bet.timeline.BetTimelineListViewRoute
import com.felipearpa.tyche.home.HomeRoute
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute

fun NavGraphBuilder.poolHomeNavView(
    navController: NavController,
    initialRoute: Any,
) {
    composable<PoolHomeViewRoute> { navBackStackEntry ->
        val route: PoolHomeViewRoute = navBackStackEntry.toRoute()
        PoolHomeView(
            poolId = route.poolId,
            gamblerId = route.gamblerId,
            onPoolChange = {
                navController.navigate(route = PoolScoreListRoute(gamblerId = route.gamblerId)) {
                    popUpTo<PoolHomeViewRoute> { inclusive = true }
                }
            },
            onSignOut = {
                navController.navigate(route = HomeRoute) {
                    popUpTo(route = initialRoute) { inclusive = true }
                }
            },
            onGamblerOpen = { _, tappedGamblerId, tappedGamblerUsername ->
                if (tappedGamblerId != route.gamblerId) {
                    navController.navigate(
                        route = BetTimelineListViewRoute(
                            poolId = route.poolId,
                            gamblerId = tappedGamblerId,
                            gamblerUsername = tappedGamblerUsername,
                        ),
                    )
                }
            },
            onMatchOpen = { poolGamblerBet ->
                navController.navigate(
                    route = MatchBetListViewRoute(
                        poolId = poolGamblerBet.poolId,
                        gamblerId = poolGamblerBet.gamblerId,
                        matchId = poolGamblerBet.matchId,
                    ),
                )
            },
        )
    }
}
