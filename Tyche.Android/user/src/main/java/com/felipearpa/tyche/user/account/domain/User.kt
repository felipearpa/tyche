package com.felipearpa.tyche.user.account.domain

import com.felipearpa.tyche.user.type.Password
import com.felipearpa.tyche.user.type.Username

data class User(
    val username: Username,
    val password: Password
)