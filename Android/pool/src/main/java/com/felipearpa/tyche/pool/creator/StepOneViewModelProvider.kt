package com.felipearpa.tyche.pool.creator

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun stepOneViewModel(): StepOneViewModel = koinViewModel()
