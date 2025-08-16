package com.felipearpa.tyche.pool.creator

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.poolFromLayoutCreatorView(
    gamblerId: String,
    onPoolCreated: (poolId: String) -> Unit,
    onBackClick: () -> Unit,
) {
    composable<PoolFromLayoutCreatorRoute> {
        PoolFromLayoutCreatorView(
            viewModel = poolFromLayoutCreatorViewModel(gamblerId = gamblerId),
            onPoolCreated = onPoolCreated,
            onBackClick = onBackClick,
        )
    }
}
