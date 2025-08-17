package com.felipearpa.tyche.account.byemail

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun emailLinkSignInViewModel(): EmailLinkSignInViewModel {
    return hiltViewModel()
}
