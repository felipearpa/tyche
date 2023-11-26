package com.felipearpa.data.pool.domain

internal data class PoolGamblerScoreResponse(
    val poolId: String,
    val poolName: String,
    val gamblerId: String,
    val gamblerUsername: String,
    val currentPosition: Int?,
    val beforePosition: Int?,
    val score: Int?
)