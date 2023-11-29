package com.felipearpa.session.managment.domain

import com.felipearpa.session.type.Password
import com.felipearpa.session.type.Username

data class Account(
    val username: Username,
    val password: Password
)