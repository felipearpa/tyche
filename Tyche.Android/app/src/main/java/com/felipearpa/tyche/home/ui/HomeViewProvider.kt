package com.felipearpa.tyche.home.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.homeView(
    onSignInWithEmail: () -> Unit,
    onSignInWithEmailAndPassword: () -> Unit
) {
    composable(route = HomeRoute.route) {
        HomeView(
            onSignInWithEmail = onSignInWithEmail,
            onSignInWithEmailAndPassword = onSignInWithEmailAndPassword
        )
    }
}