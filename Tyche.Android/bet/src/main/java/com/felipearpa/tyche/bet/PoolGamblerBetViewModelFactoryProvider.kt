package com.felipearpa.tyche.bet

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface PoolGamblerBetViewModelFactoryProvider {
    fun poolGamblerBetListViewModelFactory(): PoolGamblerBetListViewModelFactory

    fun poolGamblerBetViewModelFactory(): PoolGamblerBetItemViewModelFactory
}