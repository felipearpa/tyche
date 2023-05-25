package com.felipearpa.user.account.application

import com.felipearpa.user.User

fun CreateUserInput.toUser() =
    User(username = this.username, password = this.password)