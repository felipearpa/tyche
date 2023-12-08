package com.felipearpa.tyche.session.authentication.domain

import com.felipearpa.tyche.session.type.Password
import com.felipearpa.tyche.core.type.Email

data class LoginCredential(
    val email: Email,
    val password: Password
)