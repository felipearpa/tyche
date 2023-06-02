package com.felipearpa.tyche.user.account.domain

data class CreateUserRequest(
    val username: String,
    val password: String
)