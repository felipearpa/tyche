package com.felipearpa.tyche.account.byemail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.account.R
import com.felipearpa.tyche.session.authentication.domain.EmailLinkSignInException
import com.felipearpa.tyche.ui.exception.LocalizedException

sealed class EmailLinkSignInLocalizedException : LocalizedException() {
    data object InvalidEmailLinkSignIn : EmailLinkSignInLocalizedException() {
        @Suppress("unused")
        private fun readResolve(): Any = InvalidEmailLinkSignIn

        override val errorDescription: String
            @Composable get() = stringResource(id = R.string.invalid_email_link_sign_in_failure_description)

        override val failureReason: String
            @Composable get() = stringResource(id = R.string.invalid_email_link_sign_in_failure_reason)

        override val recoverySuggestion: String
            @Composable get() = stringResource(id = R.string.invalid_email_link_sign_in_failure_recovery_suggestion)
    }
}

fun Throwable.asEmailLinkSignInLocalizedException() =
    when (this) {
        EmailLinkSignInException.InvalidEmailLink -> EmailLinkSignInLocalizedException.InvalidEmailLinkSignIn
        else -> this
    }
