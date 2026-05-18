package com.felipearpa.tyche

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun usernameEditorViewModel(): UsernameEditorViewModel = koinViewModel()
