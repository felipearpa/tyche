package com.felipearpa.tyche.account.managment

import com.felipearpa.data.account.type.Password
import com.felipearpa.data.account.type.Username

data class AccountModel(
    val username: String,
    val password: String
)

fun AccountModel.hasErrors(): Boolean {
    return !(Username.isValid(this.username) && Password.isValid(this.password))
}