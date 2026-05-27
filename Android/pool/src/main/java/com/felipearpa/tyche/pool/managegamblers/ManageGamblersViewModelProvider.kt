package com.felipearpa.tyche.pool.managegamblers

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun manageGamblersViewModel(poolId: String): ManageGamblersViewModel =
    koinViewModel { parametersOf(poolId) }
