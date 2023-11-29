package com.felipearpa.session.login.domain

import com.felipearpa.session.AccountBundle

internal fun AccountResponse.toAccountBundle() =
    AccountBundle(
        userId = this.userId,
        username = this.username
    )