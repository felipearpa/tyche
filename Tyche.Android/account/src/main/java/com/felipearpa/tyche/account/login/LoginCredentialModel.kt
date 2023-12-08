package com.felipearpa.tyche.account.login

import com.felipearpa.tyche.session.type.Password
import com.felipearpa.tyche.core.type.Email

data class LoginCredentialModel(
    val email: String,
    val password: String
)

fun LoginCredentialModel.isValid() = Email.isValid(this.email) && Password.isValid(this.password)