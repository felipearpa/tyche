package com.felipearpa.tyche.bet.pending

import androidx.compose.runtime.Composable
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun pendingBetListViewModel(
    poolId: String,
    gamblerId: String,
): PendingBetListViewModel = koinViewModel { parametersOf(poolId, gamblerId) }

@Composable
fun pendingBetViewModel(poolGamblerBet: PoolGamblerBetModel): PendingBetItemViewModel =
    koinViewModel(
        key = "${poolGamblerBet.poolId}:${poolGamblerBet.gamblerId}:${poolGamblerBet.matchId}",
    ) { parametersOf(poolGamblerBet) }
