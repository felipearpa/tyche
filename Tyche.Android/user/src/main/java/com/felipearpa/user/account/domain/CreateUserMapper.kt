package com.felipearpa.user.account.domain

import com.felipearpa.user.User

fun User.toCreateUserRequest() =
    CreateUserRequest(username = this.username.value, password = this.password.value)