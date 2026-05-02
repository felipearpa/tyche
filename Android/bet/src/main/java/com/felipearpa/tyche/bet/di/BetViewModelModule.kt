package com.felipearpa.tyche.bet.di

import com.felipearpa.tyche.bet.finished.FinishedBetListViewModel
import com.felipearpa.tyche.bet.match.MatchBetListViewModel
import com.felipearpa.tyche.bet.pending.PendingBetItemViewModel
import com.felipearpa.tyche.bet.pending.PendingBetListViewModel
import com.felipearpa.tyche.bet.timeline.BetTimelineListViewModel
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
        MatchBetListViewModel(
            poolId = params.get(),
            gamblerId = params.get(),
            matchId = params.get(),
            getPoolGamblerBet = get(),
            getPoolMatchGamblerBets = get(),
        )
    }

    viewModel { params ->
        BetTimelineListViewModel(
            poolId = params.get(),
            gamblerId = params.get(),
            getGamblerBetsTimeline = get(),
        )
    }

    viewModel {
        PendingBetItemViewModel(
            placeBet = get(),
        )
    }
}
