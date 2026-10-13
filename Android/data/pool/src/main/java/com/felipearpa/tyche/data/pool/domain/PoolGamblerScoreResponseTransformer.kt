package com.felipearpa.tyche.data.pool.domain

import kotlin.time.Instant

/** Throws [IllegalArgumentException] when the trend group's kickoff is not an ISO-8601 instant. */
internal fun PoolGamblerScoreResponse.toPoolGamblerScore() =
    PoolGamblerScore(
        poolId = poolId,
        poolName = poolName,
        gamblerId = gamblerId,
        gamblerUsername = gamblerUsername,
        position = position,
        beforePosition = beforePosition,
        score = score,
        gamblerCount = gamblerCount,
        trendGroup = trendGroup?.toTrendGroup(),
    )

private fun TrendGroupResponse.toTrendGroup() =
    TrendGroup(
        kickoffDateTime = Instant.parse(kickoffDateTime),
        computedMatches = computedMatches,
        totalMatches = totalMatches,
    )
