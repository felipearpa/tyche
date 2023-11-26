package com.felipearpa.tyche

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.poolHome.poolHomeView

fun NavGraphBuilder.poolHomeView(navController: NavController) {
    poolHomeView(
        onPoolScoreListRequested = { navController.navigateUp() }
    )
}