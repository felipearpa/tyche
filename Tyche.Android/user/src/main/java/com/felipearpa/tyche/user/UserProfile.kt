package com.felipearpa.tyche.user

data class UserProfile(
    val userId: String,
    val username: String
)

fun UserResponse.toUserProfile() =
    UserProfile(
        userId = this.userId,
        username = this.username
    )