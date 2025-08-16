package com.felipearpa.tyche.pool.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

@AssistedFactory
interface PoolFromLayoutCreatorViewModelFactory {
    fun create(
        @Assisted("gamblerId") gamblerId: String,
    ): PoolFromLayoutCreatorViewModel
}

fun providePoolFromLayoutCreatorViewModelFactory(
    assistedFactory: PoolFromLayoutCreatorViewModelFactory,
    gamblerId: String,
): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return assistedFactory.create(gamblerId = gamblerId) as T
        }
    }
