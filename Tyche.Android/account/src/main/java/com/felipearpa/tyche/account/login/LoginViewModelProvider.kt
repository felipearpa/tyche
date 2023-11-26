package com.felipearpa.tyche.account.login

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
internal fun loginViewModel(): LoginViewModel {
    return hiltViewModel()
}