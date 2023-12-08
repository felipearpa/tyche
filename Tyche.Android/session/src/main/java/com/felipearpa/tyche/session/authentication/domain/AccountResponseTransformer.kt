package com.felipearpa.tyche.session.authentication.domain

import com.felipearpa.tyche.session.AccountBundle

internal fun AccountResponse.toAccountBundle() =
    AccountBundle(userId = this.userId)