package com.felipearpa.tyche.user

data class UserResponse(
    val userId: String,
    val username: String
)

data class LoginResponse(
    val token: String,
    val user: UserResponse
)