package com.felipearpa.tyche.poolhome.drawer

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun drawerViewModel(poolId: String, gamblerId: String): DrawerViewModel =
    koinViewModel { parametersOf(poolId, gamblerId) }
