package com.felipearpa.tyche.user.login.domain

sealed class LoginException : Throwable() {

    object InvalidCredentials : LoginException()
}