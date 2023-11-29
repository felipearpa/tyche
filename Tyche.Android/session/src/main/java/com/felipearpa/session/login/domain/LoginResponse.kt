package com.felipearpa.session.login.domain

internal data class LoginResponse(
    val token: String,
    val user: AccountResponse
)