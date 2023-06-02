package com.felipearpa.tyche.user.account.ui

sealed class UserCreationAppException : Throwable() {

    object UserAlreadyRegisteredCreation : UserCreationAppException()
}