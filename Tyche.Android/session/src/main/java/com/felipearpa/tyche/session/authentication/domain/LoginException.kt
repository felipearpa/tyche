package com.felipearpa.tyche.session.authentication.domain

sealed class LoginException : Throwable() {
    data object InvalidCredential : LoginException()
}