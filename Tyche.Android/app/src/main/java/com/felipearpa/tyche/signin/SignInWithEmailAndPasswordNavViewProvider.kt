package com.felipearpa.tyche.signin

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.account.authentication.emailAndPasswordSignInView
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute

fun NavGraphBuilder.signInWithEmailAndPasswordNavView(
    navController: NavController,
    initialRoute: Any,
) {
    emailAndPasswordSignInView(
        onBack = { navController.navigateUp() },
        onAuthenticate = { accountBundle ->
            navController.navigate(route = PoolScoreListRoute(gamblerId = accountBundle.accountId)) {
                popUpTo(route = initialRoute) { inclusive = true }
            }
        },
    )
}
