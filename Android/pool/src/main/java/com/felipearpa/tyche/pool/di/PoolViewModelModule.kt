package com.felipearpa.tyche.pool.di

import com.felipearpa.tyche.pool.creator.PoolFromLayoutCreatorViewModel
import com.felipearpa.tyche.pool.creator.StepOneViewModel
import com.felipearpa.tyche.pool.gamblerscore.GamblerScoreListViewModel
import com.felipearpa.tyche.pool.joiner.PoolJoinerViewModel
import com.felipearpa.tyche.pool.poolscore.PoolScoreListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val poolViewModelModule = module {
    viewModel { PoolJoinerViewModel(getPoolUseCase = get(), joinPoolUseCase = get()) }
    viewModel { StepOneViewModel(getOpenPoolLayoutsUseCase = get()) }

    viewModel { params ->
        PoolScoreListViewModel(
            gamblerId = params.get(),
            getPoolGamblerScoresByGamblerUseCase = get(),
            joinPoolUrlTemplate = get(),
        )
    }

    viewModel { params ->
        GamblerScoreListViewModel(
            poolId = params.get(),
            gamblerId = params.get(),
            getPoolGamblerScoresByPoolUseCase = get(),
        )
    }

    viewModel { params ->
        PoolFromLayoutCreatorViewModel(
            createPoolUseCase = get(),
            gamblerId = params.get(),
        )
    }
}
