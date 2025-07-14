package com.felipearpa.tyche.data.pool.domain

import kotlinx.serialization.Serializable

@Serializable
data class JoinPoolRequest(
    val poolId: String,
    val gamblerId: String,
)
