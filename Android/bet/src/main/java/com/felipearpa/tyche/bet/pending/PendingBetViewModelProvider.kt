package com.felipearpa.tyche.bet.pending

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun pendingBetListViewModel(
    poolId: String,
    gamblerId: String,
): PendingBetListViewModel = koinViewModel { parametersOf(poolId, gamblerId) }
