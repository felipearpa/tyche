package com.felipearpa.tyche.account.authentication

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.felipearpa.tyche.session.AccountBundle

fun NavGraphBuilder.emailAndPasswordSignInView(
    onBack: () -> Unit,
    onAuthenticate: (AccountBundle) -> Unit,
) {
    composable<EmailAndPasswordSignInRoute> {
        EmailAndPasswordSignInView(
            viewModel = emailAndPasswordSignInViewModel(),
            onBack = onBack,
            onAuthenticate = onAuthenticate,
        )
    }
}
