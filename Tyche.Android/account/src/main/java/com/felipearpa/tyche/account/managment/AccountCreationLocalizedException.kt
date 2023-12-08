package com.felipearpa.tyche.account.managment

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.session.managment.domain.AccountCreationException
import com.felipearpa.tyche.account.R
import com.felipearpa.tyche.ui.exception.LocalizedException

sealed class AccountCreationLocalizedException : LocalizedException() {
    data object AccountAlreadyRegistered : AccountCreationLocalizedException()

    override val errorDescription: String?
        @Composable get() =
            when (this) {
                AccountAlreadyRegistered ->
                    stringResource(id = R.string.account_already_registered_failure_description)
            }

    override val failureReason: String?
        @Composable get() =
            when (this) {
                AccountAlreadyRegistered ->
                    stringResource(id = R.string.account_already_registered_failure_reason)
            }

    override val recoverySuggestion: String?
        @Composable get() =
            when (this) {
                AccountAlreadyRegistered ->
                    stringResource(id = R.string.account_already_registered_failure_recovery_suggestion)
            }
}

fun Throwable.toAccountCreationLocalizedException() =
    when (this) {
        AccountCreationException.AccountAlreadyRegistered -> AccountCreationLocalizedException.AccountAlreadyRegistered
        else -> this
    }