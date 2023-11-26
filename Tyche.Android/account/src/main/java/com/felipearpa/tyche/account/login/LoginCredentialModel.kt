package com.felipearpa.tyche.account.login

import com.felipearpa.data.account.type.Password
import com.felipearpa.data.account.type.Username

data class LoginCredentialModel(
    val username: String,
    val password: String
)

fun LoginCredentialModel.hasErrors(): Boolean {
    return !(Username.isValid(this.username) && Password.isValid(this.password))
}