package com.felipearpa.tyche.session.authentication.domain

import com.felipearpa.tyche.session.authentication.infrastructure.FirebaseLoginRequest

internal fun LoginCredential.toLoginRequest() =
    LoginRequest(email = this.email.value)

internal fun LoginCredential.toFirebaseLoginRequest() =
    FirebaseLoginRequest(
        email = this.email.value,
        password = this.password.value
    )