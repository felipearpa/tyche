package com.felipearpa.tyche.pool

import com.felipearpa.tyche.core.type.KickoffGroupProgress
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScore

fun PoolGamblerScore.toPoolGamblerScoreModel() =
    PoolGamblerScoreModel(
        poolId = poolId,
        poolName = poolName,
        gamblerId = gamblerId,
        gamblerUsername = gamblerUsername,
        position = position,
        beforePosition = beforePosition,
        score = score,
        gamblerCount = gamblerCount,
        trendGroup = trendGroup?.let {
            KickoffGroupProgress(computedMatches = it.computedMatches, totalMatches = it.totalMatches)
        },
    )
