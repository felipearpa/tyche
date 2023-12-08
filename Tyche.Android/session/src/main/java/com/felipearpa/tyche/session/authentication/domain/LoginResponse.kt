package com.felipearpa.tyche.session.authentication.domain

internal data class LoginResponse(
    val token: String,
    val user: AccountResponse
)