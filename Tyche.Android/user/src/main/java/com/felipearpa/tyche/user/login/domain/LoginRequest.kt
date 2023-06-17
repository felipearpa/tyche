package com.felipearpa.tyche.user.login.domain

data class LoginRequest(val username: String, val password: String)

fun LoginCredential.toLoginRequest() =
    LoginRequest(
        username = this.username.value,
        password = this.password.value
    )