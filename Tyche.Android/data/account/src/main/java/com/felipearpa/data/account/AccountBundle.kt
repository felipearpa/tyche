package com.felipearpa.data.account

import kotlinx.serialization.Serializable

@Serializable
data class AccountBundle(
    val userId: String,
    val username: String
)