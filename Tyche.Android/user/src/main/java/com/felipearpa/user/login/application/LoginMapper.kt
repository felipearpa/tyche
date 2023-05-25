package com.felipearpa.user.login.application

import com.felipearpa.user.User

fun LoginInput.toUser() =
    User(username = this.username, password = this.password)