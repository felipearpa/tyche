package com.felipearpa.tyche.user.login.ui

import com.felipearpa.tyche.user.login.application.LoginInput
import com.felipearpa.tyche.user.type.Password
import com.felipearpa.tyche.user.type.Username

fun User.toLoginCommand() =
    LoginInput(username = Username(this.username), password = Password(this.password))