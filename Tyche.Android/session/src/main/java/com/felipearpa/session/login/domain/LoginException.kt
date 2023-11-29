package com.felipearpa.session.login.domain

sealed class LoginException : Throwable() {
    data object InvalidCredential : LoginException()
}