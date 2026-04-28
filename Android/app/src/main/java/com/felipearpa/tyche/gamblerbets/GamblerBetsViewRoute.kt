package com.felipearpa.tyche.gamblerbets

import kotlinx.serialization.Serializable

@Serializable
data class GamblerBetsViewRoute(
    val poolId: String,
    val gamblerId: String,
    val gamblerUsername: String,
)
