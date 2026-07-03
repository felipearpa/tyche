package com.felipearpa.tyche.data.pool.domain

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
    )
