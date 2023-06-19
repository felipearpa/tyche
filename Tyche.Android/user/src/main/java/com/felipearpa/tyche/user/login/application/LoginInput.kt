package com.felipearpa.tyche.user.login.application

import com.felipearpa.tyche.user.login.ui.LoginCredentialModel
import com.felipearpa.tyche.user.type.Password
import com.felipearpa.tyche.user.type.Username

data class LoginInput(
    val username: Username,
    val password: Password
)

fun LoginCredentialModel.toLoginInput() =
    LoginInput(username = Username(this.username), password = Password(this.password))