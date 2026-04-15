package com.felipearpa.tyche.pool.creator

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

fun NavGraphBuilder.poolFromLayoutCreatorView(
    onPoolCreated: (poolId: String) -> Unit,
    onBackClick: () -> Unit,
) {
    composable<PoolFromLayoutCreatorRoute> {
        val route = it.toRoute<PoolFromLayoutCreatorRoute>()
        PoolFromLayoutCreatorView(
            viewModel = poolFromLayoutCreatorViewModel(gamblerId = route.gamblerId),
            onPoolCreated = onPoolCreated,
            onBackClick = onBackClick,
        )
    }
}
