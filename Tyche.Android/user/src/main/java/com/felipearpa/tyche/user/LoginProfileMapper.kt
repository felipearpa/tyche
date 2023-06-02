package com.felipearpa.tyche.user

fun LoginResponse.toProfile() =
    LoginProfile(
        token = this.token,
        user = this.user.toProfile()
    )