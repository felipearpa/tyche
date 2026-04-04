package com.felipearpa.tyche.account.byemail

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun emailLinkSignInViewModel(): EmailLinkSignInViewModel = koinViewModel()
