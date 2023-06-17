package com.felipearpa.tyche.user.login.domain

import com.felipearpa.tyche.user.login.application.LoginInput
import com.felipearpa.tyche.user.type.Password
import com.felipearpa.tyche.user.type.Username

data class LoginCredential(
    val username: Username,
    val password: Password
)

fun LoginInput.toLoginCredential() =
    LoginCredential(
        username = this.username,
        password = this.password
    )