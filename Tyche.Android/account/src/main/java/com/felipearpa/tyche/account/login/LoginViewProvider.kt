package com.felipearpa.tyche.account.login

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.felipearpa.data.account.AccountBundle

fun NavGraphBuilder.loginView(onLoggedId: (AccountBundle) -> Unit, onBackRequested: () -> Unit) {
    composable(route = LoginRoute.route) {
        LoginView(
            viewModel = loginViewModel(),
            onLoggedIn = onLoggedId,
            onBackRequested = onBackRequested
        )
    }
}