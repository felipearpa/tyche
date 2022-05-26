package com.pipel.tyche.pool.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.AssistedFactory

@AssistedFactory
interface PoolViewModelFactory {

    fun create(pooLayoutId: String): PoolViewModel

}

fun providePoolViewModelFactory(
    assistedFactory: PoolViewModelFactory,
    pooLayoutId: String
): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return assistedFactory.create(pooLayoutId = pooLayoutId) as T
        }

    }
