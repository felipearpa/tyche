package com.felipearpa.tyche.bet.match

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.felipearpa.tyche.bet.timeline.BetTimelineListViewRoute

fun NavGraphBuilder.matchBetListNavView(
    navController: NavController,
    signedInGamblerId: String?,
) {
    composable<MatchBetListViewRoute> { navBackStackEntry ->
        val route: MatchBetListViewRoute = navBackStackEntry.toRoute()
        MatchBetListView(
            viewModel = matchBetListViewModel(
                poolId = route.poolId,
                gamblerId = route.gamblerId,
                matchId = route.matchId,
            ),
            onBack = { navController.navigateUp() },
            onGamblerOpen = { tappedPoolId, tappedGamblerId, tappedGamblerUsername ->
                if (tappedGamblerId != signedInGamblerId) {
                    navController.navigate(
                        route = BetTimelineListViewRoute(
                            poolId = tappedPoolId,
                            gamblerId = tappedGamblerId,
                            gamblerUsername = tappedGamblerUsername,
                        ),
                    )
                }
            },
        )
    }
}
