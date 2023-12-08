package com.felipearpa.tyche.session.managment.domain

sealed class AccountCreationException : Throwable() {
    data object AccountAlreadyRegistered : AccountCreationException()
}