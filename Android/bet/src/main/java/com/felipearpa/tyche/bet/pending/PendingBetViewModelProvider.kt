package com.felipearpa.tyche.bet.pending

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import dagger.hilt.android.EntryPointAccessors
import java.util.UUID

@Composable
fun pendingBetListViewModel(
    poolId: String,
    gamblerId: String
): PendingBetListViewModel {
    val factory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        PendingBetViewModelFactoryProvider::class.java
    ).poolGamblerBetListViewModelFactory()
    return viewModel(
        factory = providePendingBetListViewModelFactory(
            assistedFactory = factory,
            poolId = poolId,
            gamblerId = gamblerId
        )
    )
}

@Composable
fun pendingBetViewModel(poolGamblerBet: PoolGamblerBetModel): PendingBetItemViewModel {
    val factory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        PendingBetViewModelFactoryProvider::class.java
    ).poolGamblerBetViewModelFactory()
    return viewModel(
        factory = providePendingBetItemViewModelFactory(
            assistedFactory = factory,
            poolGamblerBet = poolGamblerBet
        ),
        key = UUID.randomUUID().toString()
    )
}