package com.felipearpa.tyche.account.authentication

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.account.R
import com.felipearpa.tyche.session.authentication.domain.SignInWithEmailAndPasswordException
import com.felipearpa.tyche.ui.exception.LocalizedException

sealed class EmailAndPasswordSignInLocalizedException : LocalizedException() {
    data object InvalidCredentials : EmailLinkSignInLocalizedException() {
        override val errorDescription: String
            @Composable get() = stringResource(id = R.string.invalid_credential_sign_in_failure_description)

        override val failureReason: String
            @Composable get() = stringResource(id = R.string.invalid_credential_sign_in_failure_reason)

        override val recoverySuggestion: String
            @Composable get() = stringResource(id = R.string.invalid_credential_sign_in_failure_recovery_suggestion)
    }

    data object AuthenticationFailed : EmailLinkSignInLocalizedException() {
        override val errorDescription: String
            @Composable get() = stringResource(id = R.string.authentication_failed_sign_in_failure_description)

        override val failureReason: String
            @Composable get() = stringResource(id = R.string.authentication_failed_sign_in_failure_reason)

        override val recoverySuggestion: String
            @Composable get() = stringResource(id = R.string.authentication_failed_sign_in_failure_recovery_suggestion)
    }
}

fun Throwable.asEmailAndPasswordSignInLocalized() =
    when (this) {
        SignInWithEmailAndPasswordException.InvalidCredentials -> EmailAndPasswordSignInLocalizedException.InvalidCredentials
        SignInWithEmailAndPasswordException.AuthenticationFailed -> EmailAndPasswordSignInLocalizedException.AuthenticationFailed
        else -> this
    }