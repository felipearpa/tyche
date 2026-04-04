package com.felipearpa.tyche.poolscore.drawer

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun drawerViewModel(): DrawerViewModel = koinViewModel()
