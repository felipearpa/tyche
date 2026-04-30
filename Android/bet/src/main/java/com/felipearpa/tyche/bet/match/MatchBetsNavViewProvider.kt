package com.felipearpa.tyche.bet.match

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.datetime.LocalDateTime

fun NavGraphBuilder.matchBetsNavView(navController: NavController) {
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
        )
    }
}
