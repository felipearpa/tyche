package com.felipearpa.user

import com.felipearpa.user.type.Password
import com.felipearpa.user.type.Username

data class User(
    val username: Username,
    val password: Password
)