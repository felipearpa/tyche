package com.felipearpa.tyche.account.managment

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
internal fun accountCreationViewModel(): AccountCreationViewModel {
    return hiltViewModel()
}