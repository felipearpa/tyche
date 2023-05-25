package com.felipearpa.user.login.ui

sealed class LoginAppException : Throwable() {

    object InvalidCredential : LoginAppException()

}