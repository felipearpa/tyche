package com.felipearpa.tyche.user

fun UserResponse.toProfile() =
    UserProfile(
        userId = this.userId,
        username = this.username
    )