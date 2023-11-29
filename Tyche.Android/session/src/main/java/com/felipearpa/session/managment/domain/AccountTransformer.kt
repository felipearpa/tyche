package com.felipearpa.session.managment.domain

internal fun Account.toCreateUserRequest() =
    CreateAccountRequest(username = this.username.value, password = this.password.value)