package com.felipearpa.tyche.account.authentication

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.emailSignInView(onBackRequested: () -> Unit) {
    composable(route = EmailSignInRoute.route) {
        EmailSignInView(
            viewModel = emailSignInViewModel(),
            onBackRequested = onBackRequested
        )
    }
}