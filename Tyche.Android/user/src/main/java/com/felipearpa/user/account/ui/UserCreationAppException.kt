package com.felipearpa.user.account.ui

sealed class UserCreationAppException : Throwable() {

    object UserAlreadyRegisteredCreation : UserCreationAppException()
}