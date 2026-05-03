package com.felipearpa.tyche.bet.timeline

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.felipearpa.tyche.bet.match.MatchBetListViewRoute

fun NavGraphBuilder.betTimelineListView(
    navController: NavController,
    onHome: () -> Unit,
) {
    composable<BetTimelineListViewRoute> { navBackStackEntry ->
        val route: BetTimelineListViewRoute = navBackStackEntry.toRoute()
        BetTimelineListView(
            poolId = route.poolId,
            gamblerId = route.gamblerId,
            gamblerUsername = route.gamblerUsername,
            onBack = { navController.navigateUp() },
            onHome = onHome,
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
