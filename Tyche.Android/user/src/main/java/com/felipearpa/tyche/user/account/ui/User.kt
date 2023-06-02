package com.felipearpa.tyche.user.account.ui

import com.felipearpa.tyche.user.type.Password
import com.felipearpa.tyche.user.type.Username

data class User(
    val username: String,
    val password: String
)

fun User.hasErrors(): Boolean {
    return !(Username.hasPattern(this.username) && Password.hasPattern(this.password))
}