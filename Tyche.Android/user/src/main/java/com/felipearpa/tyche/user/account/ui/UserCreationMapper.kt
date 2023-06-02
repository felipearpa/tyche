package com.felipearpa.tyche.user.account.ui

import com.felipearpa.tyche.user.account.application.CreateUserInput
import com.felipearpa.tyche.user.type.Password
import com.felipearpa.tyche.user.type.Username

fun User.toCreateUserCommand() =
    CreateUserInput(username = Username(this.username), password = Password(this.password))