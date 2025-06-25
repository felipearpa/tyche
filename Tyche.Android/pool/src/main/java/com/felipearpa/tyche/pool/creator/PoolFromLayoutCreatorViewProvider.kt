package com.felipearpa.tyche.pool.creator

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.poolFromLayoutCreatorView() {
    composable<PoolFromLayoutCreatorRoute> { navBackStackEntry ->
        PoolFromLayoutCreatorView(viewModel = poolFromLayoutCreatorViewModel())
    }
}
