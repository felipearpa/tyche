package com.felipearpa.user.login.domain

import com.felipearpa.user.User

fun User.toLoginRequest() =
    LoginRequest(username = this.username.value, password = this.password.value)