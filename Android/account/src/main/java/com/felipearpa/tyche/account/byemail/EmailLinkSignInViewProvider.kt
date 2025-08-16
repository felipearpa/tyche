package com.felipearpa.tyche.account.byemail

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.felipearpa.tyche.session.AccountBundle

fun NavGraphBuilder.emailLinkSignInView(
    emailLink: String,
    onStartRequested: (AccountBundle) -> Unit,
) {
    composable<EmailLinkSignInRoute>(
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "felipearpa.github.io/tyche/signin/{email}"
            },
        ),
    ) { navBackStackEntry ->
        val email = navBackStackEntry.arguments?.getString("email")!!
        EmailLinkSignInView(
            viewModel = emailLinkSignInViewModel(),
            email = email,
            emailLink = emailLink,
            onStart = onStartRequested,
        )
    }
}
