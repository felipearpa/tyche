package com.felipearpa.tyche.pool

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
    )
