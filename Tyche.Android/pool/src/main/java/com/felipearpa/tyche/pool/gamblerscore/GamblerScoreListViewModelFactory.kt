package com.felipearpa.tyche.pool.gamblerscore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

@AssistedFactory
interface GamblerScoreListViewModelFactory {
    fun create(
        @Assisted("poolId") poolId: String,
        @Assisted("gamblerId") gamblerId: String
    ): GamblerScoreListViewModel
}

fun provideGamblerScoreListViewModelFactory(
    assistedFactory: GamblerScoreListViewModelFactory,
    poolId: String,
    gamblerId: String
): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return assistedFactory.create(poolId = poolId, gamblerId = gamblerId) as T
        }
    }