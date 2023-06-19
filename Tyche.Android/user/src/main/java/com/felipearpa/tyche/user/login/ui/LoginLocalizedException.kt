package com.felipearpa.tyche.user.login.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.ui.LocalizedException
import com.felipearpa.tyche.user.R
import com.felipearpa.tyche.user.login.domain.LoginException

sealed class LoginLocalizedException : LocalizedException() {

    object InvalidCredential : LoginLocalizedException()

    override val errorDescription: String?
        @Composable get() =
            when (this) {
                InvalidCredential ->
                    stringResource(id = R.string.invalid_credential_failure_title)
            }

    override val failureReason: String?
        @Composable get() =
            when (this) {
                InvalidCredential ->
                    stringResource(id = R.string.invalid_credential_failure_message)
            }
}

fun LoginException.toLoginLocalizedException() =
    when (this) {
        LoginException.InvalidCredentials -> LoginLocalizedException.InvalidCredential
    }