package com.felipearpa.tyche.pool.poolscore

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.EntryPointAccessors

@Composable
fun poolScoreListViewModel(gamblerId: String): PoolScoreListViewModel {
    val factory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        PoolScoreListViewModelFactoryProvider::class.java
    ).poolScoreListViewModelFactory()
    return viewModel(
        factory = providePoolScoreListViewModelFactory(
            assistedFactory = factory,
            gamblerId = gamblerId
        )
    )
}