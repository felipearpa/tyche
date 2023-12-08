package com.felipearpa.tyche.account.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.session.authentication.domain.LoginException
import com.felipearpa.tyche.account.R
import com.felipearpa.tyche.ui.exception.LocalizedException

sealed class LoginLocalizedException : LocalizedException() {
    data object InvalidCredential : LoginLocalizedException()

    override val errorDescription: String?
        @Composable get() =
            when (this) {
                InvalidCredential ->
                    stringResource(id = R.string.invalid_credential_failure_description)
            }

    override val failureReason: String?
        @Composable get() =
            when (this) {
                InvalidCredential ->
                    stringResource(id = R.string.invalid_credential_failure_reason)
            }

    override val recoverySuggestion: String?
        @Composable get() =
            when (this) {
                InvalidCredential ->
                    stringResource(id = R.string.invalid_credential_recovery_suggestion)
            }
}

fun Throwable.toLoginLocalizedException() =
    when (this) {
        LoginException.InvalidCredential -> LoginLocalizedException.InvalidCredential
        else -> this
    }