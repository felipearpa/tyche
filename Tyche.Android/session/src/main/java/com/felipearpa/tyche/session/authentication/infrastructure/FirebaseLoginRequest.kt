package com.felipearpa.tyche.session.authentication.infrastructure

internal data class FirebaseLoginRequest(
    val email: String,
    val password: String
)