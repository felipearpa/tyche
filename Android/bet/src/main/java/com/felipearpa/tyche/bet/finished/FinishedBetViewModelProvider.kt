package com.felipearpa.tyche.bet.finished

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun finishedBetListViewModel(
    poolId: String,
    gamblerId: String,
): FinishedBetListViewModel =
    // Keyed by context, so another pool or gambler never reuses this pool's total.
    koinViewModel(key = "$poolId:$gamblerId") { parametersOf(poolId, gamblerId) }
