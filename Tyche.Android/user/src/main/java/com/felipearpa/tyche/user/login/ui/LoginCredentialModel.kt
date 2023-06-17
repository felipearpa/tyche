package com.felipearpa.tyche.user.login.ui

import com.felipearpa.tyche.user.UserProfile
import com.felipearpa.tyche.user.type.Password
import com.felipearpa.tyche.user.type.Username

data class LoginCredentialModel(
    val username: String,
    val password: String
)

fun LoginCredentialModel.hasErrors(): Boolean {
    return !(Username.isValid(this.username) && Password.isValid(this.password))
}