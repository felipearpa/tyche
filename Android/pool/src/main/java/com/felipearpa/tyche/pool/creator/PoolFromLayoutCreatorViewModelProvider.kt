package com.felipearpa.tyche.pool.creator

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun poolFromLayoutCreatorViewModel(gamblerId: String): PoolFromLayoutCreatorViewModel =
    koinViewModel { parametersOf(gamblerId) }
