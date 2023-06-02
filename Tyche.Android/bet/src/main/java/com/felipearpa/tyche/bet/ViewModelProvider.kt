package com.felipearpa.tyche.bet

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.felipearpa.tyche.bet.ui.PoolGamblerBetItemViewModel
import com.felipearpa.tyche.bet.ui.PoolGamblerBetListViewModel
import com.felipearpa.tyche.bet.ui.PoolGamblerBetModel
import com.felipearpa.tyche.bet.ui.providePoolGamblerBetItemViewModelFactory
import com.felipearpa.tyche.bet.ui.providePoolGamblerBetListViewModelFactory
import dagger.hilt.android.EntryPointAccessors
import java.util.UUID

@Composable
fun poolGamblerBetListViewModel(
    poolId: String,
    gamblerId: String
): PoolGamblerBetListViewModel {
    val factory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        ViewModelFactoryProvider::class.java
    ).poolGamblerBetListViewModelFactory()
    return viewModel(
        factory = providePoolGamblerBetListViewModelFactory(
            assistedFactory = factory,
            poolId = poolId,
            gamblerId = gamblerId
        )
    )
}

@Composable
fun poolGamblerBetViewModel(poolGamblerBet: PoolGamblerBetModel): PoolGamblerBetItemViewModel {
    val factory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        ViewModelFactoryProvider::class.java
    ).poolGamblerBetViewModelFactory()
    return viewModel(
        factory = providePoolGamblerBetItemViewModelFactory(
            assistedFactory = factory,
            poolGamblerBet = poolGamblerBet
        ),
        key = UUID.randomUUID().toString()
    )
}