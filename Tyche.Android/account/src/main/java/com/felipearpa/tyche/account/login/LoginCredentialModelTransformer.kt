package com.felipearpa.tyche.account.login

import com.felipearpa.tyche.session.authentication.domain.LoginCredential
import com.felipearpa.tyche.session.type.Password
import com.felipearpa.tyche.core.type.Email

fun LoginCredentialModel.toLoginCredential() =
    LoginCredential(
        email = Email(this.email),
        password = Password(this.password)
    )