package com.felipearpa.tyche.account.authentication

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.emailSignInView(onBackRequested: () -> Unit) {
    composable<EmailSignInRoute> {
        EmailSignInView(
            viewModel = emailSignInViewModel(),
            onBack = onBackRequested,
        )
    }
}
