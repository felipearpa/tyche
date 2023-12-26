package com.felipearpa.tyche.account.authentication

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
internal fun signInWithEmailViewModel(): SignInWithEmailViewModel {
    return hiltViewModel()
}