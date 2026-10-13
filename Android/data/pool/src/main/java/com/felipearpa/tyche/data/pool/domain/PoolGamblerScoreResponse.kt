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
    val gamblerCount: Int?,
    /** JSON null, or absent, while the pool has no settled kickoff group. */
    val trendGroup: TrendGroupResponse? = null,
)

/** [kickoffDateTime] is an ISO-8601 UTC instant. */
@Serializable
internal data class TrendGroupResponse(
    val kickoffDateTime: String,
    val computedMatches: Int,
    val totalMatches: Int,
)
