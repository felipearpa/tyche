package com.felipearpa.tyche.account.managment

import com.felipearpa.session.managment.domain.Account
import com.felipearpa.session.type.Password
import com.felipearpa.session.type.Username

fun AccountModel.toAccount() =
    Account(
        username = Username(this.username),
        password = Password(this.password)
    )