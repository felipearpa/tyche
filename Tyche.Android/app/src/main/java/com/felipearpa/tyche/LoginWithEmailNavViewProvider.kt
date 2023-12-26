package com.felipearpa.tyche

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.account.authentication.signInWithEmailView

fun NavGraphBuilder.loginWithEmailNavView(navController: NavController) {
    signInWithEmailView(onBackRequested = { navController.navigateUp() })
}