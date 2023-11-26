package com.felipearpa.data.account.managment.domain

import com.felipearpa.data.account.type.Password
import com.felipearpa.data.account.type.Username

data class Account(
    val username: Username,
    val password: Password
)