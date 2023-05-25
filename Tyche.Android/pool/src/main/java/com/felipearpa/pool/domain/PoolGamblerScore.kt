package com.felipearpa.pool.domain

data class PoolGamblerScore(
    val poolId: String,
    val poolLayoutId: String,
    val poolName: String,
    val gamblerId: String,
    val gamblerUsername: String,
    val currentPosition: Int?,
    val beforePosition: Int?,
    val score: Int?
)