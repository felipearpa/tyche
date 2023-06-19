package com.felipearpa.tyche.user.account.domain

fun User.toCreateUserRequest() =
    CreateUserRequest(username = this.username.value, password = this.password.value)