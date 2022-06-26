package com.pipel.tyche.poolGambler.data

data class PoolGamblerResponse(
    val poolId: String,
    val gamblerId: String,
    val gamblerEmail: String,
    val score: Int?,
    val currentPosition: Int?,
    val beforePosition: Int?
)