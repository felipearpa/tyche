package com.felipearpa.tyche.signin

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.felipearpa.tyche.account.byemail.EmailLinkSignInRoute
import com.felipearpa.tyche.account.byemail.EmailLinkSignInView
import com.felipearpa.tyche.account.byemail.emailLinkSignInViewModel
import com.felipearpa.tyche.pool.poolscore.PoolScoreListRoute

fun NavGraphBuilder.signInWithEmailLinkNavView(
    navController: NavController,
    initialRoute: Any,
    emailLink: String,
) {
    composable<EmailLinkSignInRoute>(
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "tyche-588ce.web.app/sign-in/{email}"
            },
        ),
    ) { navBackStackEntry ->
        val email = navBackStackEntry.arguments?.getString("email")!!
        EmailLinkSignInView(
            viewModel = emailLinkSignInViewModel(),
            email = email,
            emailLink = emailLink,
            onStart = { accountBundle ->
                navController.navigate(route = PoolScoreListRoute(gamblerId = accountBundle.accountId)) {
                    popUpTo(route = initialRoute) { inclusive = true }
                }
            },
        )
    }
}
