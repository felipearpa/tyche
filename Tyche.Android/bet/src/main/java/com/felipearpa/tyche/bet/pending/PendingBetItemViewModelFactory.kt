package com.felipearpa.tyche.bet.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import dagger.assisted.AssistedFactory

@AssistedFactory
interface PendingBetItemViewModelFactory {
    fun create(poolGamblerBet: PoolGamblerBetModel): PendingBetItemViewModel
}

fun providePendingBetItemViewModelFactory(
    assistedFactory: PendingBetItemViewModelFactory,
    poolGamblerBet: PoolGamblerBetModel
): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return assistedFactory.create(poolGamblerBet = poolGamblerBet) as T
        }
    }