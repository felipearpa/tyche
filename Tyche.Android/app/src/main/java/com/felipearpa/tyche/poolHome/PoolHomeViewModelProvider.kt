package com.felipearpa.tyche.poolHome

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.EntryPointAccessors

@Composable
fun poolHomeViewModel(poolId: String, gamblerId: String): PoolHomeViewModel {
    val factory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        PoolHomeViewModelFactoryProvider::class.java
    ).poolHomeViewModelFactory()
    return viewModel(
        factory = providePoolHomeViewModelFactory(
            assistedFactory = factory,
            poolId = poolId,
            gamblerId = gamblerId
        )
    )
}