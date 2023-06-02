package com.felipearpa.tyche.pool.ui

import com.felipearpa.tyche.pool.domain.PoolGamblerScore

fun PoolGamblerScore.toModel() =
    PoolGamblerScoreModel(
        poolId = this.poolId,
        poolName = this.poolName,
        gamblerId = this.gamblerId,
        gamblerUsername = this.gamblerUsername,
        currentPosition = this.currentPosition,
        beforePosition = this.beforePosition,
        score = this.score
    )