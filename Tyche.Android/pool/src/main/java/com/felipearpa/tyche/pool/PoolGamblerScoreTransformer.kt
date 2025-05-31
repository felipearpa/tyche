package com.felipearpa.tyche.pool

import com.felipearpa.tyche.data.pool.domain.PoolGamblerScore

fun PoolGamblerScore.toPoolGamblerScoreModel() =
    PoolGamblerScoreModel(
        poolId = this.poolId,
        poolName = this.poolName,
        gamblerId = this.gamblerId,
        gamblerUsername = this.gamblerUsername,
        currentPosition = this.currentPosition,
        beforePosition = this.beforePosition,
        score = this.score
    )
