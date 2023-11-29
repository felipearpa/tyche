package com.felipearpa.session.login.domain

import com.felipearpa.session.AuthBundle
import com.felipearpa.session.LoginBundle

internal fun LoginResponse.toLoginBundle() =
    LoginBundle(
        authBundle = AuthBundle(token = this.token),
        accountBundle = this.user.toAccountBundle()
    )