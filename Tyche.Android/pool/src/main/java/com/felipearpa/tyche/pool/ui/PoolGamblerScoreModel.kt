package com.felipearpa.tyche.pool.ui

import com.felipearpa.tyche.pool.domain.PoolGamblerScore

data class PoolGamblerScoreModel(
    val poolId: String,
    val poolName: String,
    val gamblerId: String,
    val gamblerUsername: String,
    val currentPosition: Int?,
    val beforePosition: Int?,
    val score: Int?
) {

    fun calculateDifference(): Int? {
        currentPosition?.let { nonNullCurrentPosition ->
            beforePosition?.let { bp ->
                return bp - nonNullCurrentPosition
            }
            return nonNullCurrentPosition
        }
        return null
    }
}

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