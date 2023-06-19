package com.felipearpa.tyche.user.account.ui

import com.felipearpa.tyche.user.account.application.CreateUserInput
import com.felipearpa.tyche.user.type.Password
import com.felipearpa.tyche.user.type.Username

fun UserModel.toCreateUserCommand() =
    CreateUserInput(username = Username(this.username), password = Password(this.password))