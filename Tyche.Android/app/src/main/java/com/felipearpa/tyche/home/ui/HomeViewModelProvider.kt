package com.felipearpa.tyche.home.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun homeViewModel(): HomeViewModel {
    return hiltViewModel()
}