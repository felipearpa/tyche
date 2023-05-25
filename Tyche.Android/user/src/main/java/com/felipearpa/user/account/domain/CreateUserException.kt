package com.felipearpa.user.account.domain

sealed class CreateUserException : Throwable() {

    object UserAlreadyRegistered : CreateUserException()
}