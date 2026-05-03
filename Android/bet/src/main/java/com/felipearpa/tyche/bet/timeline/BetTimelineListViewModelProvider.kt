package com.felipearpa.tyche.bet.timeline

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun betTimelineListViewModel(
    poolId: String,
    gamblerId: String,
): BetTimelineListViewModel = koinViewModel { parametersOf(poolId, gamblerId) }
