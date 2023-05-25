package com.felipearpa.user

fun UserResponse.toProfile() =
    UserProfile(
        userId = this.userId,
        username = this.username
    )