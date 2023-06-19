package com.felipearpa.tyche.user.account.application

import com.felipearpa.tyche.user.account.ui.UserModel
import com.felipearpa.tyche.user.type.Password
import com.felipearpa.tyche.user.type.Username

data class CreateUserInput(
    val username: Username,
    val password: Password
)

fun UserModel.toCreateUserInput() =
    CreateUserInput(username = Username(this.username), password = Password(this.password))