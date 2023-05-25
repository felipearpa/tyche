package com.felipearpa.tyche

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.felipearpa.tyche.poolHome.PoolHomeViewModel
import com.felipearpa.tyche.poolHome.providePoolHomeViewModelFactory
import dagger.hilt.android.EntryPointAccessors

@Composable
fun poolHomeViewModel(poolId: String, gamblerId: String): PoolHomeViewModel {
    val factory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        ViewModelFactoryProvider::class.java
    ).poolHomeViewModelFactory()
    return viewModel(
        factory = providePoolHomeViewModelFactory(
            assistedFactory = factory,
            poolId = poolId,
            gamblerId = gamblerId
        )
    )
}