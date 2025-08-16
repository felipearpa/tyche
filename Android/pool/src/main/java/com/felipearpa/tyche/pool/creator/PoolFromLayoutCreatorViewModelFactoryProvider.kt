package com.felipearpa.tyche.pool.creator

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface PoolFromLayoutCreatorViewModelFactoryProvider {
    fun poolFromLayoutCreatorViewModelFactory(): PoolFromLayoutCreatorViewModelFactory
}
