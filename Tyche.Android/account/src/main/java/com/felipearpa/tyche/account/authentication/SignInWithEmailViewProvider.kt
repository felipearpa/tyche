package com.felipearpa.tyche.account.authentication

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.signInWithEmailView(onBackRequested: () -> Unit) {
    composable(route = SignInWithEmailRoute.route) {
        SignInWithEmailView(
            viewModel = signInWithEmailViewModel(),
            onBackRequested = onBackRequested
        )
    }
}