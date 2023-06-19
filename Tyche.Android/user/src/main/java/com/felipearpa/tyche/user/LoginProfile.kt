package com.felipearpa.tyche.user

data class LoginProfile(
    val token: String,
    val user: UserProfile
)

fun LoginResponse.toLoginProfile() =
    LoginProfile(
        token = this.token,
        user = this.user.toUserProfile()
    )