package com.felipearpa.tyche.pool.di

import com.felipearpa.tyche.pool.creator.PoolFromLayoutCreatorViewModel
import com.felipearpa.tyche.pool.creator.StepOneViewModel
import com.felipearpa.tyche.pool.gamblerscore.GamblerScoreListViewModel
import com.felipearpa.tyche.pool.joiner.PoolJoinerViewModel
import com.felipearpa.tyche.pool.managegamblers.ManageGamblersViewModel
import com.felipearpa.tyche.pool.poolscore.PoolScoreListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val poolViewModelModule = module {
    viewModel { PoolJoinerViewModel(getPool = get(), joinPoolAction = get()) }
    viewModel { StepOneViewModel(getOpenPoolLayouts = get()) }

    viewModel { params ->
        PoolScoreListViewModel(
            gamblerId = params.get(),
            getPoolGamblerScoresByGambler = get(),
            getOpenPoolLayouts = get(),
            joinPoolUrlTemplate = get(),
        )
    }

    viewModel { params ->
        GamblerScoreListViewModel(
            poolId = params.get(),
            gamblerId = params.get(),
            getPoolGamblerScoresByPool = get(),
        )
    }

    viewModel { params ->
        PoolFromLayoutCreatorViewModel(
            createPool = get(),
            gamblerId = params.get(),
        )
    }

    viewModel { params ->
        ManageGamblersViewModel(
            poolId = params.get(),
            getPoolMembers = get(),
            removeGambler = get(),
        )
    }
}
