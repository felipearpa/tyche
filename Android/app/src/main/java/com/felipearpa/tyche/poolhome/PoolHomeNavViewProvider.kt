package com.felipearpa.tyche.poolhome

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.felipearpa.tyche.bet.match.MatchBetListViewRoute
import com.felipearpa.tyche.bet.timeline.BetTimelineListViewRoute
import com.felipearpa.tyche.home.HomeRoute
import com.felipearpa.tyche.pool.managegamblers.ManageGamblersRoute
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute
import com.felipearpa.tyche.profile.ProfileRoute
import com.felipearpa.tyche.ui.runIfStarted

fun NavGraphBuilder.poolHomeNavView(
    navController: NavController,
    initialRoute: Any,
) {
    composable<PoolHomeViewRoute> { navBackStackEntry ->
        val route: PoolHomeViewRoute = navBackStackEntry.toRoute()
        val openPoolList = {
            navController.navigate(route = PoolScoreListRoute(gamblerId = route.gamblerId)) {
                popUpTo<PoolHomeViewRoute> { inclusive = true }
            }
        }

        // The user actions that leave this route run only while it is started, so an action
        // activated twice before pool home recomposes navigates once. Sign-out is guarded in the
        // drawer, together with the logout it starts.
        PoolHomeView(
            poolId = route.poolId,
            gamblerId = route.gamblerId,
            onPoolChange = { navBackStackEntry.runIfStarted(openPoolList) },
            // Not guarded: a deletion finishes by itself, and still reaches the pool list if the
            // gambler has opened another screen or left the app in the meantime.
            onPoolDeleted = openPoolList,
            onSignOut = {
                navController.navigate(route = HomeRoute) {
                    popUpTo(route = initialRoute) { inclusive = true }
                }
            },
            onManageGamblers = {
                navBackStackEntry.runIfStarted {
                    navController.navigate(
                        route = ManageGamblersRoute(
                            poolId = route.poolId,
                            gamblerId = route.gamblerId,
                        ),
                    )
                }
            },
            onProfile = { navBackStackEntry.runIfStarted { navController.navigate(route = ProfileRoute) } },
            onGamblerOpen = { _, tappedGamblerId, tappedGamblerUsername ->
                if (tappedGamblerId != route.gamblerId) {
                    navBackStackEntry.runIfStarted {
                        navController.navigate(
                            route = BetTimelineListViewRoute(
                                poolId = route.poolId,
                                gamblerId = tappedGamblerId,
                                gamblerUsername = tappedGamblerUsername,
                            ),
                        )
                    }
                }
            },
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
