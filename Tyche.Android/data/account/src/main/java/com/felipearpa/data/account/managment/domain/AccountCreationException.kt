package com.felipearpa.data.account.managment.domain

sealed class AccountCreationException : Throwable() {
    data object AccountAlreadyRegistered : AccountCreationException()
}