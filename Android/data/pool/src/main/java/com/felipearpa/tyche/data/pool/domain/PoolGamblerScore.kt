package com.felipearpa.tyche.data.pool.domain

data class PoolGamblerScore(
    val poolId: String,
    val poolName: String,
    val gamblerId: String,
    val gamblerUsername: String,
    val currentPosition: Int?,
    val beforePosition: Int?,
    val score: Int?
)
