package com.felipearpa.tyche.account.authentication

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.account.R
import com.felipearpa.tyche.session.authentication.domain.SignInWithEmailLinkException
import com.felipearpa.tyche.ui.exception.LocalizedException

sealed class SignInWithEmailLinkLocalizedException : LocalizedException() {
    data object AuthenticationFailed : SignInWithEmailLinkLocalizedException()
    data object InvalidEmailLink : SignInWithEmailLinkLocalizedException()

    override val errorDescription: String?
        @Composable get() =
            when (this) {
                AuthenticationFailed ->
                    stringResource(id = R.string.sign_in_with_email_link_authentication_failure_description)

                InvalidEmailLink ->
                    stringResource(id = R.string.sign_in_with_email_link_invalid_link_failure_description)
            }

    override val failureReason: String?
        @Composable get() =
            when (this) {
                AuthenticationFailed ->
                    stringResource(id = R.string.sign_in_with_email_link_authentication_failure_reason)

                InvalidEmailLink ->
                    stringResource(id = R.string.sign_in_with_email_link_invalid_link_failure_reason)
            }

    override val recoverySuggestion: String?
        @Composable get() =
            when (this) {
                AuthenticationFailed ->
                    stringResource(id = R.string.sign_in_with_email_link_authentication_failure_recovery_suggestion)

                InvalidEmailLink ->
                    stringResource(id = R.string.sign_in_with_email_link_invalid_link_failure_recovery_suggestion)
            }
}

fun Throwable.asSignInWithEmailLinkLocalizedException() =
    when (this) {
        SignInWithEmailLinkException.AuthenticationFailed -> SignInWithEmailLinkLocalizedException.AuthenticationFailed
        SignInWithEmailLinkException.InvalidEmailLink -> SignInWithEmailLinkLocalizedException.InvalidEmailLink
        else -> this
    }