package com.felipearpa.tyche.account.byemailandpassword

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun emailAndPasswordSignInViewModel(): EmailAndPasswordSignInViewModel = koinViewModel()
