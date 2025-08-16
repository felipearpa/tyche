package com.felipearpa.tyche.pool.poolscore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.AssistedFactory

@AssistedFactory
interface PoolScoreListViewModelFactory {
    fun create(gamblerId: String): PoolScoreListViewModel
}

fun providePoolScoreListViewModelFactory(
    assistedFactory: PoolScoreListViewModelFactory,
    gamblerId: String
): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return assistedFactory.create(gamblerId = gamblerId) as T
        }
    }