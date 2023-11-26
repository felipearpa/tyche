package com.felipearpa.tyche.account.managment

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.felipearpa.data.account.AccountBundle

fun NavGraphBuilder.accountCreationView(
    onAccountCreated: (AccountBundle) -> Unit,
    onBackRequested: () -> Unit
) {
    composable(route = AccountCreationRoute.route) {
        AccountCreationView(
            viewModel = accountCreationViewModel(),
            onAccountCreated = onAccountCreated,
            onBackRequested = onBackRequested
        )
    }
}