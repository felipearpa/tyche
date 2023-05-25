package com.felipearpa.bet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.AssistedFactory

@AssistedFactory
interface PoolGamblerBetItemViewModelFactory {

    fun create(poolGamblerBet: PoolGamblerBetModel): PoolGamblerBetItemViewModel
}

fun providePoolGamblerBetItemViewModelFactory(
    assistedFactory: PoolGamblerBetItemViewModelFactory,
    poolGamblerBet: PoolGamblerBetModel
): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return assistedFactory.create(poolGamblerBet = poolGamblerBet) as T
        }
    }