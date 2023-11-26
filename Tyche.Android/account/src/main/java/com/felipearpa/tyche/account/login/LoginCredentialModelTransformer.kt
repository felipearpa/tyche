package com.felipearpa.tyche.account.login

import com.felipearpa.data.account.login.domain.LoginCredential
import com.felipearpa.data.account.type.Password
import com.felipearpa.data.account.type.Username

fun LoginCredentialModel.toLoginCredential() =
    LoginCredential(
        username = Username(this.username),
        password = Password(this.password)
    )