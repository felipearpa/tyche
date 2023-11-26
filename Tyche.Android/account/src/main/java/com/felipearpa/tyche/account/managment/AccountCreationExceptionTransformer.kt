package com.felipearpa.tyche.account.managment

import com.felipearpa.data.account.managment.domain.AccountCreationException

fun Throwable.toAccountCreationLocalizedExceptionOnMatch() =
    when (this) {
        AccountCreationException.AccountAlreadyRegistered -> AccountCreationLocalizedException.AccountAlreadyRegistered
        else -> this
    }