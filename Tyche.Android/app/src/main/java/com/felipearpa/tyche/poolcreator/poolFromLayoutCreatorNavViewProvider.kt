package com.felipearpa.tyche.poolcreator

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.pool.creator.poolFromLayoutCreatorView

fun NavGraphBuilder.poolFromLayoutCreatorNavView(gamblerId: String, navController: NavController) {
    poolFromLayoutCreatorView(gamblerId = gamblerId, onBackClick = { navController.popBackStack() })
}
