package com.felipearpa.tyche.account.managment

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.data.account.managment.domain.AccountCreationException
import com.felipearpa.tyche.account.R
import com.felipearpa.tyche.ui.exception.LocalizedException

sealed class AccountCreationLocalizedException : LocalizedException() {
    data object AccountAlreadyRegistered : AccountCreationLocalizedException()

    override val errorDescription: String?
        @Composable get() =
            when (this) {
                AccountAlreadyRegistered ->
                    stringResource(id = R.string.user_already_registered_failure_title)
            }

    override val failureReason: String?
        @Composable get() =
            when (this) {
                AccountAlreadyRegistered ->
                    stringResource(id = R.string.user_already_registered_failure_message)
            }
}

fun Throwable.toAccountCreationLocalizedExceptionOnMatch() =
    when (this) {
        AccountCreationException.AccountAlreadyRegistered -> AccountCreationLocalizedException.AccountAlreadyRegistered
        else -> this
    }