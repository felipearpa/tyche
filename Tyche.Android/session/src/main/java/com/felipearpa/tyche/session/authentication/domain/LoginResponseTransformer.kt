package com.felipearpa.tyche.session.authentication.domain

import com.felipearpa.tyche.session.AuthBundle
import com.felipearpa.tyche.session.LoginBundle

internal fun LoginResponse.toLoginBundle() =
    LoginBundle(
        authBundle = AuthBundle(authToken = this.token),
        accountBundle = this.user.toAccountBundle()
    )