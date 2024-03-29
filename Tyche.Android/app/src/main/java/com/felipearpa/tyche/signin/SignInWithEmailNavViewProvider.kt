package com.felipearpa.tyche.signin

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.account.authentication.emailSignInView

fun NavGraphBuilder.signInWithEmailNavView(navController: NavController) {
    emailSignInView(onBackRequested = { navController.navigateUp() })
}