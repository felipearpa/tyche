package com.felipearpa.tyche.bet.live

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun liveBetListViewModel(
    poolId: String,
    gamblerId: String,
): LiveBetListViewModel = koinViewModel { parametersOf(poolId, gamblerId) }
