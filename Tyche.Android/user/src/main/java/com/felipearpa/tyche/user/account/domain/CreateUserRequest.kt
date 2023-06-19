package com.felipearpa.tyche.user.account.domain

data class CreateUserRequest(
    val username: String,
    val password: String
)

fun User.toCreateUserRequest() =
    CreateUserRequest(username = this.username.value, password = this.password.value)