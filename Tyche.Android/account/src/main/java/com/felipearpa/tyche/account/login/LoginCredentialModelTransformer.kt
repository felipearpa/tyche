package com.felipearpa.tyche.account.login

import com.felipearpa.session.login.domain.LoginCredential
import com.felipearpa.session.type.Password
import com.felipearpa.session.type.Username

fun LoginCredentialModel.toLoginCredential() =
    LoginCredential(
        username = Username(this.username),
        password = Password(this.password)
    )