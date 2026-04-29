package com.felipearpa.tyche.gamblerbets

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.felipearpa.tyche.bet.match.MatchBetsViewRoute

fun NavGraphBuilder.gamblerBetsNavView(navController: NavController) {
    composable<GamblerBetsViewRoute> { navBackStackEntry ->
        val route: GamblerBetsViewRoute = navBackStackEntry.toRoute()
        GamblerBetsView(
            poolId = route.poolId,
            gamblerId = route.gamblerId,
            gamblerUsername = route.gamblerUsername,
            onBack = { navController.navigateUp() },
            onMatchOpen = { poolGamblerBet ->
                navController.navigate(
                    route = MatchBetsViewRoute(
                        poolId = poolGamblerBet.poolId,
                        matchId = poolGamblerBet.matchId,
                        homeTeamName = poolGamblerBet.homeTeamName,
                        awayTeamName = poolGamblerBet.awayTeamName,
                        matchDateTimeIso = poolGamblerBet.matchDateTime.toString(),
                        homeTeamScore = poolGamblerBet.matchScore?.homeTeamValue,
                        awayTeamScore = poolGamblerBet.matchScore?.awayTeamValue,
                    ),
                )
            },
        )
    }
}
