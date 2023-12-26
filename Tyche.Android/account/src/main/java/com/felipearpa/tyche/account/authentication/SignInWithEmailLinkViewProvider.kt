package com.felipearpa.tyche.account.authentication

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.felipearpa.tyche.session.AccountBundle

fun NavGraphBuilder.signInWithEmailLinkView(
    emailLink: String,
    onStartRequested: (AccountBundle) -> Unit
) {
    composable(
        route = SignInWithEmailLinkRoute.route,
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "felipearpa.github.io/tyche/signin/{email}"
            }
        )
    ) { navBackStackEntry ->
        val email = navBackStackEntry.arguments?.getString("email")!!
        SignInWithEmailLinkView(
            viewModel = signInWithEmailLinkViewModel(),
            email = email,
            emailLink = emailLink,
            onStartRequested = onStartRequested
        )
    }
}