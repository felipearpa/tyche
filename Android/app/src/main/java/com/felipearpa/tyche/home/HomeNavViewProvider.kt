package com.felipearpa.tyche.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.felipearpa.tyche.account.byemail.EmailSignInRoute
import com.felipearpa.tyche.account.byemailandpassword.EmailAndPasswordSignInRoute
import com.felipearpa.tyche.account.social.SocialSignInRow
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute

fun NavGraphBuilder.homeNavView(navController: NavController) {
    composable<HomeRoute> {
        HomeView(
            onSignInWithEmail = { navController.navigate(route = EmailSignInRoute) },
            onSignInWithEmailAndPassword = { navController.navigate(route = EmailAndPasswordSignInRoute) },
            socialSignInSlot = {
                SocialSignInRow(
                    onAuthenticate = { accountBundle ->
                        navController.navigate(route = PoolScoreListRoute(gamblerId = accountBundle.accountId)) {
                            popUpTo(route = HomeRoute) { inclusive = true }
                        }
                    },
                )
            },
        )
    }
}
