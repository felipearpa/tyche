package com.felipearpa.tyche.user.account.domain

import com.felipearpa.tyche.user.account.application.CreateUserInput
import com.felipearpa.tyche.user.type.Password
import com.felipearpa.tyche.user.type.Username

data class User(
    val username: Username,
    val password: Password
)

fun CreateUserInput.toUser() =
    User(username = this.username, password = this.password)