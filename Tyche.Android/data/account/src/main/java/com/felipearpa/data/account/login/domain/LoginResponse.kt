package com.felipearpa.data.account.login.domain

internal data class LoginResponse(
    val token: String,
    val user: UserResponse
)