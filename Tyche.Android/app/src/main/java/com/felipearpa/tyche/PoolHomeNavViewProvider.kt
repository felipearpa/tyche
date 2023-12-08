package com.felipearpa.tyche

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.home.ui.HomeRoute
import com.felipearpa.tyche.poolhome.poolHomeView

fun NavGraphBuilder.poolHomeView(navController: NavController, initialRoute: String) {
    poolHomeView(
        onPoolScoreListRequested = { navController.navigateUp() },
        onLogout = {
            navController.navigate(route = HomeRoute.route()) {
                popUpTo(route = initialRoute) { inclusive = true }
            }
        }
    )
}