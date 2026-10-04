package com.felipearpa.tyche.bet.timeline

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.felipearpa.tyche.bet.match.MatchBetListViewRoute
import com.felipearpa.tyche.ui.runIfStarted

fun NavGraphBuilder.betTimelineListView(
    navController: NavController,
    onHome: () -> Unit,
) {
    composable<BetTimelineListViewRoute> { navBackStackEntry ->
        val route: BetTimelineListViewRoute = navBackStackEntry.toRoute()
        // Each action leaves this route at most once, even when activated twice in one frame.
        BetTimelineListView(
            poolId = route.poolId,
            gamblerId = route.gamblerId,
            gamblerUsername = route.gamblerUsername,
            onBack = { navBackStackEntry.runIfStarted { navController.navigateUp() } },
            onHome = { navBackStackEntry.runIfStarted(onHome) },
            onMatchOpen = { poolGamblerBet ->
                navBackStackEntry.runIfStarted {
                    navController.navigate(
                        route = MatchBetListViewRoute(
                            poolId = poolGamblerBet.poolId,
                            gamblerId = poolGamblerBet.gamblerId,
                            matchId = poolGamblerBet.matchId,
                        ),
                    )
                }
            },
        )
    }
}
