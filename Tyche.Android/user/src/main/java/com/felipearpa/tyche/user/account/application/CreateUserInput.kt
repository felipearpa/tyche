package com.felipearpa.tyche.user.account.application

import com.felipearpa.tyche.user.type.Password
import com.felipearpa.tyche.user.type.Username

data class CreateUserInput(
    val username: Username,
    val password: Password
)