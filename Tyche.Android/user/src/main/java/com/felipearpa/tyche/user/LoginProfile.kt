package com.felipearpa.tyche.user

data class LoginProfile(
    val token: String,
    val user: UserProfile
)