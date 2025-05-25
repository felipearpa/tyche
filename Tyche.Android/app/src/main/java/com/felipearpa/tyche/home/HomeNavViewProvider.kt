package com.felipearpa.tyche.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.account.authentication.EmailAndPasswordSignInRoute
import com.felipearpa.tyche.account.authentication.EmailSignInRoute
import com.felipearpa.tyche.home.ui.homeView

fun NavGraphBuilder.homeNavView(navController: NavController) {
    homeView(
        onSignInWithEmail = { navController.navigate(route = EmailSignInRoute.route()) },
        onSignInWithEmailAndPassword = { navController.navigate(route = EmailAndPasswordSignInRoute.route()) },
    )
}