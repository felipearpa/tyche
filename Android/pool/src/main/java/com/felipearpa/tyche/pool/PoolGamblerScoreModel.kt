package com.felipearpa.tyche.pool

data class PoolGamblerScoreModel(
    val poolId: String,
    val poolName: String,
    val gamblerId: String,
    val gamblerUsername: String,
    val currentPosition: Int?,
    val beforePosition: Int?,
    val score: Int?
)

fun PoolGamblerScoreModel.difference(): Int? {
    val currentPosition = this.currentPosition ?: return null
    val beforePosition = this.beforePosition ?: return null
    return beforePosition - currentPosition
}