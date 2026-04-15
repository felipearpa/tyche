package com.felipearpa.tyche.pool

data class PoolGamblerScoreModel(
    val poolId: String,
    val poolName: String,
    val gamblerId: String,
    val gamblerUsername: String,
    val position: Int?,
    val beforePosition: Int?,
    val score: Int?,
)

fun PoolGamblerScoreModel.difference(): Int? {
    val currentPosition = this.position ?: return null
    val beforePosition = this.beforePosition ?: return null
    return beforePosition - currentPosition
}
