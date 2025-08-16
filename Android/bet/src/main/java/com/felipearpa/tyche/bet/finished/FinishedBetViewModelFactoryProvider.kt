package com.felipearpa.tyche.bet.finished

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface FinishedBetViewModelFactoryProvider {
    fun finishedBetListViewModelFactory(): FinishedBetListViewModelFactory
}