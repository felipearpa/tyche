package com.felipearpa.tyche.bet.di

import com.felipearpa.tyche.bet.finished.FinishedBetListViewModel
import com.felipearpa.tyche.bet.live.LiveBetListViewModel
import com.felipearpa.tyche.bet.match.MatchBetListViewModel
import com.felipearpa.tyche.bet.pending.PendingBetItemViewModel
import com.felipearpa.tyche.bet.pending.PendingBetListViewModel
import com.felipearpa.tyche.bet.timeline.BetsTimelineViewModel
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
        LiveBetListViewModel(
            poolId = params.get(),
            gamblerId = params.get(),
            getLivePoolGamblerBets = get(),
        )
    }

    viewModel { params ->
        MatchBetListViewModel(
            poolId = params.get(),
            matchId = params.get(),
            getPoolMatchGamblerBets = get(),
        )
    }

    viewModel { params ->
        BetsTimelineViewModel(
            poolId = params.get(),
            gamblerId = params.get(),
            getGamblerBetsTimeline = get(),
        )
    }

    viewModel { params ->
        PendingBetItemViewModel(
            poolGamblerBet = params.get(),
            placeBet = get(),
        )
    }
}
