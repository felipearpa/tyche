package com.felipearpa.tyche.user.account.application

import com.felipearpa.tyche.user.account.domain.User

fun CreateUserInput.toUser() =
    User(username = this.username, password = this.password)