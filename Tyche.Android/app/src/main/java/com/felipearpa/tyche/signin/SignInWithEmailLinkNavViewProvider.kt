package com.felipearpa.tyche.signin

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.account.authentication.emailLinkSignInView
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute

fun NavGraphBuilder.signInWithEmailLinkNavView(
    navController: NavController,
    initialRoute: String,
    emailLink: String
) {
    emailLinkSignInView(
        emailLink = emailLink,
        onStartRequested = { accountBundle ->
            navController.navigate(route = PoolScoreListRoute.route(gamblerId = accountBundle.accountId)) {
                popUpTo(route = initialRoute) { inclusive = true }
            }
        }
    )
}