package com.felipearpa.tyche.bet.match

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.felipearpa.tyche.bet.timeline.BetTimelineListViewRoute
import com.felipearpa.tyche.ui.runIfStarted

/**
 * The match screen's destination. [signedInAccountId] returns the signed-in account when the
 * destination is shown; it owns "Your bet" and the "You" row, whoever the route's gambler is.
 */
fun NavGraphBuilder.matchBetListNavView(
    navController: NavController,
    signedInAccountId: () -> String?,
    onHome: () -> Unit,
) {
    composable<MatchBetListViewRoute> { navBackStackEntry ->
        val route: MatchBetListViewRoute = navBackStackEntry.toRoute()
        val viewerAccountId = checkNotNull(signedInAccountId()) { "The match screen requires a signed-in account" }
        // Each action leaves this route at most once, even when activated twice in one frame.
        MatchBetListView(
            viewModel = matchBetListViewModel(
                poolId = route.poolId,
                gamblerId = route.gamblerId,
                matchId = route.matchId,
                viewerAccountId = viewerAccountId,
            ),
            onBack = { navBackStackEntry.runIfStarted { navController.navigateUp() } },
            onHome = { navBackStackEntry.runIfStarted(onHome) },
            onGamblerOpen = { tappedPoolId, tappedGamblerId, tappedGamblerUsername ->
                if (tappedGamblerId != viewerAccountId) {
                    navBackStackEntry.runIfStarted {
                        navController.navigate(
                            route = BetTimelineListViewRoute(
                                poolId = tappedPoolId,
                                gamblerId = tappedGamblerId,
                                gamblerUsername = tappedGamblerUsername,
                            ),
                        )
                    }
                }
            },
        )
    }
}
