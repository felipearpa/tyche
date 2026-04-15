package com.felipearpa.tyche.data.pool.domain

internal fun PoolGamblerScoreResponse.toPoolGamblerScore() =
    PoolGamblerScore(
        poolId = this.poolId,
        poolName = this.poolName,
        gamblerId = this.gamblerId,
        gamblerUsername = this.gamblerUsername,
        position = this.position,
        beforePosition = this.beforePosition,
        score = this.score,
    )
