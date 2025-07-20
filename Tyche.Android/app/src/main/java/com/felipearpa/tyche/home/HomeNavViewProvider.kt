package com.felipearpa.tyche.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.felipearpa.tyche.account.authentication.EmailAndPasswordSignInRoute
import com.felipearpa.tyche.account.authentication.EmailSignInRoute

fun NavGraphBuilder.homeNavView(navController: NavController) {
    composable<HomeRoute> {
        HomeView(
            onSignInWithEmail = { navController.navigate(route = EmailSignInRoute) },
            onSignInWithEmailAndPassword = { navController.navigate(route = EmailAndPasswordSignInRoute) },
        )
    }
}
