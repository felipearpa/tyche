package com.felipearpa.session.login.domain

internal fun LoginCredential.toLoginRequest() =
    LoginRequest(
        username = this.username.value,
        password = this.password.value
    )