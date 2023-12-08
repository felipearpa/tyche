package com.felipearpa.tyche.session.managment.domain

import com.felipearpa.tyche.session.managment.infrastructure.FirebaseAccountCreationRequest

internal fun Account.toCreateAccountRequest() =
    AccountCreationRequest(email = this.email.value)

internal fun Account.toCreateFirebaseAccountRequest() =
    FirebaseAccountCreationRequest(
        email = this.email.value,
        password = this.password.value
    )