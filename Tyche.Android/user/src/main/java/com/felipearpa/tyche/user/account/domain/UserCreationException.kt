package com.felipearpa.tyche.user.account.domain

sealed class UserCreationException : Throwable() {

    object UserAlreadyRegistered : UserCreationException()
}