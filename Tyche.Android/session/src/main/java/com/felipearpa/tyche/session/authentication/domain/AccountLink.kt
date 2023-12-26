package com.felipearpa.tyche.session.authentication.domain

import com.felipearpa.tyche.core.type.Email

data class AccountLink(
    val email: Email,
    val externalAccountId: String
)
