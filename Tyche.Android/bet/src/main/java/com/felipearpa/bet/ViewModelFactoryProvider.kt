package com.felipearpa.bet

import com.felipearpa.bet.ui.PoolGamblerBetListViewModelFactory
import com.felipearpa.bet.ui.PoolGamblerBetItemViewModelFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface ViewModelFactoryProvider {

    fun poolGamblerBetListViewModelFactory(): PoolGamblerBetListViewModelFactory

    fun poolGamblerBetViewModelFactory(): PoolGamblerBetItemViewModelFactory
}