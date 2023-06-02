package com.felipearpa.tyche.pool.ui

data class PoolGamblerScoreModel(
    val poolId: String,
    val poolLayoutId: String,
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