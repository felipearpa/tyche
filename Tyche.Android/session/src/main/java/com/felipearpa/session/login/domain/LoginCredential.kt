package com.felipearpa.session.login.domain

import com.felipearpa.session.type.Password
import com.felipearpa.session.type.Username

data class LoginCredential(
    val username: Username,
    val password: Password
)