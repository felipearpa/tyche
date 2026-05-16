package com.felipearpa.tyche.account.bygoogle

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.account.R
import com.felipearpa.tyche.session.authentication.domain.GoogleSignInException
import com.felipearpa.tyche.ui.exception.LocalizedException

sealed class GoogleSignInLocalizedException : LocalizedException() {
    data object InvalidCredential : GoogleSignInLocalizedException() {
        private fun readResolve(): Any = InvalidCredential

        override val errorDescription: String
            @Composable get() = stringResource(id = R.string.google_sign_in_invalid_credential_failure_description)

        override val failureReason: String
            @Composable get() = stringResource(id = R.string.google_sign_in_invalid_credential_failure_reason)

        override val recoverySuggestion: String
            @Composable get() = stringResource(id = R.string.google_sign_in_invalid_credential_failure_recovery_suggestion)
    }

    data object AccountExistsWithDifferentCredential : GoogleSignInLocalizedException() {
        private fun readResolve(): Any = AccountExistsWithDifferentCredential

        override val errorDescription: String
            @Composable get() = stringResource(id = R.string.google_sign_in_account_exists_failure_description)

        override val failureReason: String
            @Composable get() = stringResource(id = R.string.google_sign_in_account_exists_failure_reason)

        override val recoverySuggestion: String
            @Composable get() = stringResource(id = R.string.google_sign_in_account_exists_failure_recovery_suggestion)
    }

    data object NetworkError : GoogleSignInLocalizedException() {
        private fun readResolve(): Any = NetworkError

        override val errorDescription: String
            @Composable get() = stringResource(id = R.string.google_sign_in_network_failure_description)

        override val failureReason: String
            @Composable get() = stringResource(id = R.string.google_sign_in_network_failure_reason)

        override val recoverySuggestion: String
            @Composable get() = stringResource(id = R.string.google_sign_in_network_failure_recovery_suggestion)
    }
}

fun Throwable.asGoogleSignInLocalized() =
    when (this) {
        GoogleSignInException.InvalidCredential -> GoogleSignInLocalizedException.InvalidCredential
        GoogleSignInException.AccountExistsWithDifferentCredential -> GoogleSignInLocalizedException.AccountExistsWithDifferentCredential
        GoogleSignInException.NetworkError -> GoogleSignInLocalizedException.NetworkError
        else -> this
    }
