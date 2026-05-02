package com.felipearpa.tyche.bet.match

import kotlinx.serialization.Serializable

@Serializable
data class MatchBetListViewRoute(
    val poolId: String,
    val gamblerId: String,
    val matchId: String,
)
