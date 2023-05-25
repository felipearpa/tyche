package com.felipearpa.pool

import com.felipearpa.pool.ui.gamblerScore.GamblerScoreListViewModelFactory
import com.felipearpa.pool.ui.poolScore.PoolScoreListViewModelFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface ViewModelFactoryProvider {

    fun poolScoreListViewModelFactory(): PoolScoreListViewModelFactory

    fun gamblerScoreListViewModelFactory(): GamblerScoreListViewModelFactory
}