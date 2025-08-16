package com.felipearpa.tyche.poolcreator

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.felipearpa.tyche.pool.creator.poolFromLayoutCreatorView

fun NavGraphBuilder.poolFromLayoutCreatorNavView(navController: NavController, gamblerId: String) {
    poolFromLayoutCreatorView(
        gamblerId = gamblerId,
        onPoolCreated = { navController.popBackStack() },
        onBackClick = { navController.popBackStack() },
    )
}
