package com.felipearpa.user.account.domain

data class CreateUserRequest(
    val username: String,
    val password: String
)