package com.felipearpa.tyche

import com.felipearpa.tyche.poolHome.PoolHomeViewModelFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface ViewModelFactoryProvider {

    fun poolHomeViewModelFactory(): PoolHomeViewModelFactory
}