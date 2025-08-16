package com.felipearpa.tyche.pool.poolscore

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface PoolScoreListViewModelFactoryProvider {
    fun poolScoreListViewModelFactory(): PoolScoreListViewModelFactory
}