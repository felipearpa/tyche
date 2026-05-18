package com.felipearpa.tyche.session.authentication.domain

import kotlinx.serialization.Serializable

@Serializable
internal data class UpdateUsernameRequest(
    val accountId: String,
    val username: String,
)
