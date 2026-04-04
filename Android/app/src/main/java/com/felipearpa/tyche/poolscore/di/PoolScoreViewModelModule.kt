package com.felipearpa.tyche.poolscore.di

import com.felipearpa.tyche.poolscore.drawer.DrawerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val poolScoreViewModelModule = module {
    viewModel { DrawerViewModel(logoutUseCase = get()) }
}
