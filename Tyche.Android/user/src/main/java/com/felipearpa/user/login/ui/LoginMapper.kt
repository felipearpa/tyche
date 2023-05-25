package com.felipearpa.user.login.ui

import com.felipearpa.user.login.application.LoginInput
import com.felipearpa.user.type.Password
import com.felipearpa.user.type.Username

fun User.toLoginCommand() =
    LoginInput(username = Username(this.username), password = Password(this.password))