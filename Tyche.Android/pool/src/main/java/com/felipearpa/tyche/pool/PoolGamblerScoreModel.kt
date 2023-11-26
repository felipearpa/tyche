package com.felipearpa.tyche.pool

data class PoolGamblerScoreModel(
    val poolId: String,
    val poolName: String,
    val gamblerId: String,
    val gamblerUsername: String,
    val currentPosition: Int?,
    val beforePosition: Int?,
    val score: Int?
) {

    fun difference(): Int? {
        currentPosition?.let { currentPosition ->
            beforePosition?.let { bp ->
                return bp - currentPosition
            }
            return currentPosition
        }
        return null
    }
}