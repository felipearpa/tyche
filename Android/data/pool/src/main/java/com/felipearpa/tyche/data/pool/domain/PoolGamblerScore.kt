package com.felipearpa.tyche.data.pool.domain

data class PoolGamblerScore(
    val poolId: String,
    val poolName: String,
    val gamblerId: String,
    val gamblerUsername: String,
    val position: Int?,
    /** The position before the [trendGroup]'s first settlement. */
    val beforePosition: Int?,
    val score: Int?,
    val gamblerCount: Int?,
    /** `null` while the pool has no settled kickoff group. */
    val trendGroup: TrendGroup?,
)
