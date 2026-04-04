package com.felipearpa.tyche.pool.gamblerscore

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun gamblerScoreListViewModel(poolId: String, gamblerId: String): GamblerScoreListViewModel =
    koinViewModel { parametersOf(poolId, gamblerId) }
