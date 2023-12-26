package com.felipearpa.tyche.session.authentication.domain

internal data class LinkAccountRequest(
    val email: String,
    val externalAccountId: String
)