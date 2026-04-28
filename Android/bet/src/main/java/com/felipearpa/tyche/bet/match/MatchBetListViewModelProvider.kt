package com.felipearpa.tyche.bet.match

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun matchBetListViewModel(
    poolId: String,
    matchId: String,
): MatchBetListViewModel = koinViewModel { parametersOf(poolId, matchId) }
