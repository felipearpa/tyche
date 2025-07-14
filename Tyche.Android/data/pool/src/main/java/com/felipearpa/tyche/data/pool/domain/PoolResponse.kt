package com.felipearpa.tyche.data.pool.domain

import kotlinx.serialization.Serializable

@Serializable
internal data class PoolResponse(
    val poolId: String,
    val poolName: String,
)
