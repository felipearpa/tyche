package com.felipearpa.user.account.ui

import com.felipearpa.user.account.application.CreateUserInput
import com.felipearpa.user.type.Password
import com.felipearpa.user.type.Username

fun User.toCreateUserCommand() =
    CreateUserInput(username = Username(this.username), password = Password(this.password))