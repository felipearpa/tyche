package com.felipearpa.tyche.poolhome.di

import com.felipearpa.tyche.poolhome.drawer.DrawerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val poolHomeViewModelModule = module {
    viewModel { params ->
        DrawerViewModel(
            poolId = params.get(),
            gamblerId = params.get(),
            logOut = get(),
            getPoolGamblerScore = get(),
        )
    }
}
