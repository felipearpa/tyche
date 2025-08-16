package com.felipearpa.tyche.bet.finished

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.EntryPointAccessors

@Composable
fun finishedBetListViewModel(
    poolId: String,
    gamblerId: String
): FinishedBetListViewModel {
    val factory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        FinishedBetViewModelFactoryProvider::class.java
    ).finishedBetListViewModelFactory()
    return viewModel(
        factory = provideFinishedBetListViewModelFactory(
            assistedFactory = factory,
            poolId = poolId,
            gamblerId = gamblerId
        )
    )
}