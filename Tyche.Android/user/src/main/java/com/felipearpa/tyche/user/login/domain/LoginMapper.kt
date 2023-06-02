package com.felipearpa.tyche.user.login.domain

import com.felipearpa.tyche.user.User

fun User.toLoginRequest() =
    LoginRequest(username = this.username.value, password = this.password.value)