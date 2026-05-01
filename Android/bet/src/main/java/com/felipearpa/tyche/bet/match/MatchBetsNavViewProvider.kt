package com.felipearpa.tyche.bet.match

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.felipearpa.tyche.bet.timeline.BetTimelineListViewRoute
import kotlinx.datetime.LocalDateTime

fun NavGraphBuilder.matchBetsNavView(
    navController: NavController,
    signedInGamblerId: String?,
) {
    composable<MatchBetListViewRoute> { navBackStackEntry ->
        val route: MatchBetListViewRoute = navBackStackEntry.toRoute()
        MatchBetListView(
            poolId = route.poolId,
            matchId = route.matchId,
            homeTeamName = route.homeTeamName,
            awayTeamName = route.awayTeamName,
            matchDateTime = LocalDateTime.parse(route.matchDateTimeIso),
            homeTeamScore = route.homeTeamScore,
            awayTeamScore = route.awayTeamScore,
            onBack = { navController.navigateUp() },
            isLive = route.isLive,
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
