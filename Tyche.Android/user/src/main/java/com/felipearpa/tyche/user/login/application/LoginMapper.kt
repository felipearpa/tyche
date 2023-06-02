package com.felipearpa.tyche.user.login.application

import com.felipearpa.tyche.user.User

fun LoginInput.toUser() =
    User(username = this.username, password = this.password)