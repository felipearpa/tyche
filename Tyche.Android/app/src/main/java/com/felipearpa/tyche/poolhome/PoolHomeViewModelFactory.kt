package com.felipearpa.tyche.poolhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

@AssistedFactory
interface PoolHomeViewModelFactory {
    fun create(
        @Assisted("poolId") poolId: String,
        @Assisted("gamblerId") gamblerId: String
    ): PoolHomeViewModel
}

fun providePoolHomeViewModelFactory(
    assistedFactory: PoolHomeViewModelFactory,
    poolId: String,
    gamblerId: String
): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return assistedFactory.create(poolId = poolId, gamblerId = gamblerId) as T
        }
    }