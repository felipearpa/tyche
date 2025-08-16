package com.felipearpa.tyche.pool.gamblerscore

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.EntryPointAccessors

@Composable
fun gamblerScoreListViewModel(poolId: String, gamblerId: String): GamblerScoreListViewModel {
    val factory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        GamblerScoreListViewModelFactoryProvider::class.java
    ).gamblerScoreListViewModelFactory()
    return viewModel(
        factory = provideGamblerScoreListViewModelFactory(
            assistedFactory = factory,
            poolId = poolId,
            gamblerId = gamblerId
        )
    )
}