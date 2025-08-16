package com.felipearpa.tyche.bet.pending

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface PendingBetViewModelFactoryProvider {
    fun poolGamblerBetListViewModelFactory(): PendingBetListViewModelFactory

    fun poolGamblerBetViewModelFactory(): PendingBetItemViewModelFactory
}