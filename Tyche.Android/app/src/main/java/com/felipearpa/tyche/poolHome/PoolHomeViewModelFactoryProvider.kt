package com.felipearpa.tyche.poolHome

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface PoolHomeViewModelFactoryProvider {
    fun poolHomeViewModelFactory(): PoolHomeViewModelFactory
}