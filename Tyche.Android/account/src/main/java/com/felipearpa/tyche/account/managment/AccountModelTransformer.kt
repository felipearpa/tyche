package com.felipearpa.tyche.account.managment

import com.felipearpa.tyche.session.managment.domain.Account
import com.felipearpa.tyche.session.type.Password
import com.felipearpa.tyche.core.type.Email

fun AccountModel.toAccount() =
    Account(
        email = Email(this.email),
        password = Password(this.password)
    )