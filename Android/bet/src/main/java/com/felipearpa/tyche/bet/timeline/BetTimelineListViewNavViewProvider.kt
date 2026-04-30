package com.felipearpa.tyche.bet.timeline

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.felipearpa.tyche.bet.isLive
import com.felipearpa.tyche.bet.match.MatchBetListViewRoute

fun NavGraphBuilder.betTimelineListView(navController: NavController) {
    composable<BetTimelineListViewRoute> { navBackStackEntry ->
        val route: BetTimelineListViewRoute = navBackStackEntry.toRoute()
        BetTimelineListView(
            poolId = route.poolId,
            gamblerId = route.gamblerId,
            gamblerUsername = route.gamblerUsername,
            onBack = { navController.navigateUp() },
            onMatchOpen = { poolGamblerBet ->
                navController.navigate(
                    route = MatchBetListViewRoute(
                        poolId = poolGamblerBet.poolId,
                        matchId = poolGamblerBet.matchId,
                        homeTeamName = poolGamblerBet.homeTeamName,
                        awayTeamName = poolGamblerBet.awayTeamName,
                        matchDateTimeIso = poolGamblerBet.matchDateTime.toString(),
                        homeTeamScore = poolGamblerBet.matchScore?.homeTeamValue,
                        awayTeamScore = poolGamblerBet.matchScore?.awayTeamValue,
                        isLive = poolGamblerBet.isLive,
                    ),
                )
            },
        )
    }
}
