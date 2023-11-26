package com.felipearpa.data.account.login.domain

import com.felipearpa.data.account.type.Password
import com.felipearpa.data.account.type.Username

data class LoginCredential(
    val username: Username,
    val password: Password
)