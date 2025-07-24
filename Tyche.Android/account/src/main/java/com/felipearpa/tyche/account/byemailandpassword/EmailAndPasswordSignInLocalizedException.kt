package com.felipearpa.tyche.account.byemailandpassword

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.account.R
import com.felipearpa.tyche.session.authentication.domain.EmailAndPasswordSignInException
import com.felipearpa.tyche.ui.exception.LocalizedException

sealed class EmailAndPasswordSignInLocalizedException : LocalizedException() {
    data object InvalidCredentials : EmailAndPasswordSignInLocalizedException() {
        private fun readResolve(): Any = InvalidCredentials

        override val errorDescription: String
            @Composable get() = stringResource(id = R.string.invalid_credential_sign_in_failure_description)

        override val failureReason: String
            @Composable get() = stringResource(id = R.string.invalid_credential_sign_in_failure_reason)

        override val recoverySuggestion: String
            @Composable get() = stringResource(id = R.string.invalid_credential_sign_in_failure_recovery_suggestion)
    }
}

fun Throwable.asEmailAndPasswordSignInLocalized() =
    when (this) {
        EmailAndPasswordSignInException.InvalidCredentials -> EmailAndPasswordSignInLocalizedException.InvalidCredentials
        else -> this
    }
