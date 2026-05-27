package com.felipearpa.tyche.data.pool.domain

import kotlinx.serialization.Serializable

@Serializable
internal data class PoolMemberResponse(
    val gamblerId: String,
    val gamblerUsername: String,
    val gamblerEmail: String,
    val isOwner: Boolean,
)
