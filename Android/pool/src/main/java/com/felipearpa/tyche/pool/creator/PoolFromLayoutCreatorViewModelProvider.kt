package com.felipearpa.tyche.pool.creator

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.EntryPointAccessors

@Composable
fun poolFromLayoutCreatorViewModel(gamblerId: String): PoolFromLayoutCreatorViewModel {
    val factory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        PoolFromLayoutCreatorViewModelFactoryProvider::class.java,
    ).poolFromLayoutCreatorViewModelFactory()
    return viewModel(
        factory = providePoolFromLayoutCreatorViewModelFactory(
            assistedFactory = factory,
            gamblerId = gamblerId,
        ),
    )
}
