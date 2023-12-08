package com.felipearpa.tyche.settings

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun settingsViewModel(): SettingsViewModel = hiltViewModel()