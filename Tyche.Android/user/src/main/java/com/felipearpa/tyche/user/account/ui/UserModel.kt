package com.felipearpa.tyche.user.account.ui

import com.felipearpa.tyche.user.type.Password
import com.felipearpa.tyche.user.type.Username

data class UserModel(
    val username: String,
    val password: String
)

fun UserModel.hasErrors(): Boolean {
    return !(Username.isValid(this.username) && Password.isValid(this.password))
}