package com.felipearpa.tyche.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.account.authentication.EmailSignInRoute
import com.felipearpa.tyche.home.ui.homeView

fun NavGraphBuilder.homeNavView(navController: NavController) {
    homeView(onLoginRequested = { navController.navigate(route = EmailSignInRoute.route()) })
}