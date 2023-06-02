package com.felipearpa.tyche.pool

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.felipearpa.tyche.pool.ui.gamblerScore.GamblerScoreListViewModel
import com.felipearpa.tyche.pool.ui.gamblerScore.provideGamblerScoreListViewModelFactory
import com.felipearpa.tyche.pool.ui.poolScore.PoolScoreListViewModel
import com.felipearpa.tyche.pool.ui.poolScore.providePoolScoreListViewModelFactory
import dagger.hilt.android.EntryPointAccessors

@Composable
fun poolScoreListViewModel(gamblerId: String): PoolScoreListViewModel {
    val factory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        ViewModelFactoryProvider::class.java
    ).poolScoreListViewModelFactory()
    return viewModel(
        factory = providePoolScoreListViewModelFactory(
            assistedFactory = factory,
            gamblerId = gamblerId
        )
    )
}

@Composable
fun gamblerScoreListViewModel(poolId: String, gamblerId: String): GamblerScoreListViewModel {
    val factory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        ViewModelFactoryProvider::class.java
    ).gamblerScoreListViewModelFactory()
    return viewModel(
        factory = provideGamblerScoreListViewModelFactory(
            assistedFactory = factory,
            poolId = poolId,
            gamblerId = gamblerId
        )
    )
}