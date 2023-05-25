package com.felipearpa.user.account.application

import com.felipearpa.user.type.Password
import com.felipearpa.user.type.Username

data class CreateUserInput(
    val username: Username,
    val password: Password
)