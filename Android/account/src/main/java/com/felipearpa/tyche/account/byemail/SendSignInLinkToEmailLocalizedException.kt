package com.felipearpa.tyche.account.byemail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.account.R
import com.felipearpa.tyche.session.authentication.domain.SendSignInLinkToEmailException
import com.felipearpa.tyche.ui.exception.LocalizedException

sealed class SendSignInLinkToEmailLocalizedException : LocalizedException() {
    data object TooManyRequests : SendSignInLinkToEmailLocalizedException() {
        @Suppress("unused")
        private fun readResolve(): Any = TooManyRequests

        override val errorDescription: String
            @Composable get() = stringResource(id = R.string.send_sign_in_link_to_email_too_many_requests_failure_description)

        override val failureReason: String
            @Composable get() = stringResource(id = R.string.send_sign_in_link_to_email_too_many_requests_failure_reason)

        override val recoverySuggestion: String
            @Composable get() = stringResource(id = R.string.send_sign_in_link_to_email_too_many_requests_failure_recovery_suggestion)
    }
}

fun Throwable.asSendSignInLinkToEmailLocalizedException() =
    when (this) {
        SendSignInLinkToEmailException.TooManyRequests -> SendSignInLinkToEmailLocalizedException.TooManyRequests
        else -> this
    }
