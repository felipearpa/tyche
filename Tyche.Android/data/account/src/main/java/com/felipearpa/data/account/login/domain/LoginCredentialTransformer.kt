package com.felipearpa.data.account.login.domain

internal fun LoginCredential.toLoginRequest() =
    LoginRequest(
        username = this.username.value,
        password = this.password.value
    )