package com.felipearpa.tyche.account.login

import com.felipearpa.session.type.Password
import com.felipearpa.session.type.Username

data class LoginCredentialModel(
    val username: String,
    val password: String
)

fun LoginCredentialModel.hasErrors(): Boolean {
    return !(Username.isValid(this.username) && Password.isValid(this.password))
}