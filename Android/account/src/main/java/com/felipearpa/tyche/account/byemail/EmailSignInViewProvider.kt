package com.felipearpa.tyche.account.byemail

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
