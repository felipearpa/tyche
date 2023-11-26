package com.felipearpa.data.account.login.domain

sealed class LoginException : Throwable() {
    data object InvalidCredential : LoginException()
}