package com.felipearpa.session.managment.domain

sealed class AccountCreationException : Throwable() {
    data object AccountAlreadyRegistered : AccountCreationException()
}