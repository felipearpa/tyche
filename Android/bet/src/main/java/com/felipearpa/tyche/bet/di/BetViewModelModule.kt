package com.felipearpa.tyche.bet.di

import com.felipearpa.tyche.bet.finished.FinishedBetListViewModel
import com.felipearpa.tyche.bet.pending.PendingBetItemViewModel
import com.felipearpa.tyche.bet.pending.PendingBetListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val betViewModelModule = module {
    viewModel { params ->
        PendingBetListViewModel(
            poolId = params.get(),
            gamblerId = params.get(),
            getPendingPoolGamblerBets = get(),
        )
    }

    viewModel { params ->
        FinishedBetListViewModel(
            poolId = params.get(),
            gamblerId = params.get(),
            getFinishedPoolGamblerBets = get(),
        )
    }

    viewModel { params ->
        PendingBetItemViewModel(
            poolGamblerBet = params.get(),
            placeBet = get(),
        )
    }
}
