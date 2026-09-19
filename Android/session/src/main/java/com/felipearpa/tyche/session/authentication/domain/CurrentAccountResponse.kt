package com.felipearpa.tyche.session.authentication.domain

import kotlinx.serialization.Serializable

@Serializable
internal data class CurrentAccountResponse(
    val accountId: String,
    val externalAccountId: String,
    val email: String,
    val username: String? = null,
)
