package com.felipearpa.tyche.user.account.domain

sealed class CreateUserException : Throwable() {

    object UserAlreadyRegistered : CreateUserException()
}