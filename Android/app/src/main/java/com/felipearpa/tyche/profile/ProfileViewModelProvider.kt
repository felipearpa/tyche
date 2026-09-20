package com.felipearpa.tyche.profile

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun profileViewModel(): ProfileViewModel = koinViewModel()
