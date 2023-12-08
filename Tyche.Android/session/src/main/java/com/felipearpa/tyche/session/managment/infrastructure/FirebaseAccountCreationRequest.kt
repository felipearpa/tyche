package com.felipearpa.tyche.session.managment.infrastructure

internal data class FirebaseAccountCreationRequest(
    val email: String,
    val password: String
)