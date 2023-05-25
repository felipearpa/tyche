package com.felipearpa.user.login.domain

sealed class LoginException : Throwable() {

    object InvalidCredentials : LoginException()
}