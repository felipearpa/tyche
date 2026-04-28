package com.felipearpa.tyche.matchbets

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.datetime.LocalDateTime

fun NavGraphBuilder.matchBetsNavView(navController: NavController) {
    composable<MatchBetsViewRoute> { navBackStackEntry ->
        val route: MatchBetsViewRoute = navBackStackEntry.toRoute()
        MatchBetsView(
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
