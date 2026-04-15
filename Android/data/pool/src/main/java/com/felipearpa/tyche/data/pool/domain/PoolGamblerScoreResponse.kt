package com.felipearpa.tyche.data.pool.domain

import kotlinx.serialization.Serializable

@Serializable
internal data class PoolGamblerScoreResponse(
    val poolId: String,
    val poolName: String,
    val gamblerId: String,
    val gamblerUsername: String,
    val position: Int?,
    val beforePosition: Int?,
    val score: Int?,
)
