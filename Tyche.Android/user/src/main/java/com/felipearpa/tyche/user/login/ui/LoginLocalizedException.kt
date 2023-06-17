package com.felipearpa.tyche.user.login.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.ui.LocalizedException
import com.felipearpa.tyche.user.R
import com.felipearpa.tyche.user.login.domain.LoginException

sealed class LoginLocalizedException : LocalizedException() {

    object InvalidCredential : LoginAppException()

    override val errorDescription: String?
        @Composable get() = stringResource(id = R.string.invalid_credential_failure_message)

    override val failureReason: String?
        @Composable get() = stringResource(id = R.string.invalid_credentials_failure_title)
}

fun LoginException.toLoginLocalizedException() =
    when (this) {
        LoginException.InvalidCredentials -> LoginLocalizedException.InvalidCredential
    }