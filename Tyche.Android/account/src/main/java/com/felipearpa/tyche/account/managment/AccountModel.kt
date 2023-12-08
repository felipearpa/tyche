package com.felipearpa.tyche.account.managment

import com.felipearpa.tyche.session.type.Password
import com.felipearpa.tyche.core.type.Email

data class AccountModel(
    val email: String,
    val password: String
)

fun AccountModel.isValid() = Email.isValid(this.email) && Password.isValid(this.password)