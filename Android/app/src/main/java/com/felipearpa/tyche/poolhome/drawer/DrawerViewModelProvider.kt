package com.felipearpa.tyche.poolhome.drawer

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.EntryPointAccessors

@Composable
fun drawerViewModel(poolId: String, gamblerId: String): DrawerViewModel {
    val factory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        DrawerViewModelFactoryProvider::class.java
    ).drawerViewModelFactory()
    return viewModel(
        factory = provideDrawerViewModelFactory(
            assistedFactory = factory,
            poolId = poolId,
            gamblerId = gamblerId
        )
    )
}