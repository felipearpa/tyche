package com.felipearpa.tyche.session.authentication.domain

import kotlinx.serialization.Serializable

@Serializable
internal data class LinkAccountRequest(
    val email: String,
    val externalAccountId: String,
)
