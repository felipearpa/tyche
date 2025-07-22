package com.felipearpa.tyche.account.byemailandpassword

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
internal fun emailAndPasswordSignInViewModel(): EmailAndPasswordSignInViewModel {
    return hiltViewModel()
}
