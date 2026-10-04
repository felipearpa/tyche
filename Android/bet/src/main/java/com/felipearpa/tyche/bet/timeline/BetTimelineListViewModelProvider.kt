package com.felipearpa.tyche.bet.timeline

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun betTimelineListViewModel(
    poolId: String,
    gamblerId: String,
): BetTimelineListViewModel =
    // Keyed by context, so another pool or gambler never reuses this gambler's rows or total.
    koinViewModel(key = "$poolId:$gamblerId") { parametersOf(poolId, gamblerId) }
