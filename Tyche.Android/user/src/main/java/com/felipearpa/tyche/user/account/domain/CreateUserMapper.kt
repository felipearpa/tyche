package com.felipearpa.tyche.user.account.domain

import com.felipearpa.tyche.user.User

fun User.toCreateUserRequest() =
    CreateUserRequest(username = this.username.value, password = this.password.value)