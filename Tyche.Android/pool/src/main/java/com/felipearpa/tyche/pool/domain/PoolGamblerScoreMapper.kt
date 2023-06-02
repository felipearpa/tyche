package com.felipearpa.tyche.pool.domain

fun PoolGamblerScoreResponse.toDomain() =
    PoolGamblerScore(
        poolId = this.poolId,
        poolLayoutId = this.poolLayoutId,
        poolName = this.poolName,
        gamblerId = this.gamblerId,
        gamblerUsername = this.gamblerUsername,
        currentPosition = this.currentPosition,
        beforePosition = this.beforePosition,
        score = this.score
    )