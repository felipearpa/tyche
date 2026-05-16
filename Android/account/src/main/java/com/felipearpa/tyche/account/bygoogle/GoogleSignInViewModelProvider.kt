package com.felipearpa.tyche.account.bygoogle

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun googleSignInViewModel(): GoogleSignInViewModel = koinViewModel()
