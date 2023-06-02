package com.felipearpa.tyche.user.login.ui

sealed class LoginAppException : Throwable() {

    object InvalidCredential : LoginAppException()
}