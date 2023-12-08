package com.felipearpa.tyche.session.managment.domain

internal data class AccountCreationResponse(
    val token: String,
    val user: AccountResponse
)