package com.felipearpa.tyche.pool.creator

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
internal fun stepOneViewModel(): StepOneViewModel = hiltViewModel()
