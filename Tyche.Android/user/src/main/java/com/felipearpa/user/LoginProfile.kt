package com.felipearpa.user

data class LoginProfile(
    val token: String,
    val user: UserProfile
)