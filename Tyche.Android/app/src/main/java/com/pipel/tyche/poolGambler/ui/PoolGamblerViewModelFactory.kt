package com.pipel.tyche.poolGambler.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.AssistedFactory

@AssistedFactory
interface PoolGamblerViewModelFactory {

    fun create(pooId: String): PoolGamblerViewModel

}

fun providePoolGamblerViewModelFactory(
    assistedFactory: PoolGamblerViewModelFactory,
    pooId: String
): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return assistedFactory.create(pooId = pooId) as T
        }

    }
