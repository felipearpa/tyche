package com.felipearpa.tyche.session.managment.domain

import com.felipearpa.tyche.session.type.Password
import com.felipearpa.tyche.core.type.Email

data class Account(
    val email: Email,
    val password: Password
)