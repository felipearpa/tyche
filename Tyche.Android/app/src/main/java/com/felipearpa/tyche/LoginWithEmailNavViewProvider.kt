package com.felipearpa.tyche

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.account.authentication.emailSignInView

fun NavGraphBuilder.loginWithEmailNavView(navController: NavController) {
    emailSignInView(onBackRequested = { navController.navigateUp() })
}