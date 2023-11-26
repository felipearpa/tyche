package com.felipearpa.tyche.account.managment

import com.felipearpa.data.account.managment.domain.Account
import com.felipearpa.data.account.type.Password
import com.felipearpa.data.account.type.Username

fun AccountModel.toAccount() =
    Account(
        username = Username(this.username),
        password = Password(this.password)
    )