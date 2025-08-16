package com.felipearpa.tyche.pool.gamblerscore

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface GamblerScoreListViewModelFactoryProvider {
    fun gamblerScoreListViewModelFactory(): GamblerScoreListViewModelFactory
}