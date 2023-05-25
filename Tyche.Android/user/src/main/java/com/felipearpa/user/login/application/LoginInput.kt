package com.felipearpa.user.login.application

import com.felipearpa.user.type.Password
import com.felipearpa.user.type.Username

data class LoginInput(
    val username: Username,
    val password: Password
)