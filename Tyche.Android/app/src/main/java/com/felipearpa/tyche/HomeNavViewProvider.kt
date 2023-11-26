package com.felipearpa.tyche

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.account.login.LoginRoute
import com.felipearpa.tyche.account.managment.AccountCreationRoute
import com.felipearpa.tyche.home.ui.homeView

fun NavGraphBuilder.homeView(navController: NavController) {
    homeView(
        onLoginRequested = { navController.navigate(route = LoginRoute.route()) },
        onNewAccountRequested = { navController.navigate(route = AccountCreationRoute.route()) }
    )
}