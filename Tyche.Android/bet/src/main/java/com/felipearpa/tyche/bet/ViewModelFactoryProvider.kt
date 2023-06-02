package com.felipearpa.tyche.bet

import com.felipearpa.tyche.bet.ui.PoolGamblerBetListViewModelFactory
import com.felipearpa.tyche.bet.ui.PoolGamblerBetItemViewModelFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface ViewModelFactoryProvider {

    fun poolGamblerBetListViewModelFactory(): PoolGamblerBetListViewModelFactory

    fun poolGamblerBetViewModelFactory(): PoolGamblerBetItemViewModelFactory
}