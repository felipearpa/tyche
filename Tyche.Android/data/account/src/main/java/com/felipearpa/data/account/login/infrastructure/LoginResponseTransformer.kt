package com.felipearpa.data.account.login.infrastructure

import com.felipearpa.data.account.AuthBundle
import com.felipearpa.data.account.LoginBundle
import com.felipearpa.data.account.AccountBundle
import com.felipearpa.data.account.login.domain.LoginResponse

internal fun LoginResponse.toLoginBundle() =
    LoginBundle(
        authBundle = AuthBundle(token = this.token),
        accountBundle = AccountBundle(userId = this.user.userId, username = this.user.username)
    )