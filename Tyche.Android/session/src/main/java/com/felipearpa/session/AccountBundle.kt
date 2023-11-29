package com.felipearpa.session

import kotlinx.serialization.Serializable

@Serializable
data class AccountBundle(
    val userId: String,
    val username: String
)