package com.felipearpa.tyche.poolhome

import kotlinx.serialization.Serializable

@Serializable
data class PoolHomeViewRoute(
    val poolId: String,
    val gamblerId: String,
)
