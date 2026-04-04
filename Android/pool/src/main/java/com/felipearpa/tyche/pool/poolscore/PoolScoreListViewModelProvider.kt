package com.felipearpa.tyche.pool.poolscore

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun poolScoreListViewModel(gamblerId: String): PoolScoreListViewModel = koinViewModel { parametersOf(gamblerId) }
