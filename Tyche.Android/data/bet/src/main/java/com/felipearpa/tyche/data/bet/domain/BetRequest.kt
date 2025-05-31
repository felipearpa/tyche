package com.felipearpa.tyche.data.bet.domain

import kotlinx.serialization.Serializable

@Serializable
internal data class BetRequest(
    val poolId: String,
    val gamblerId: String,
    val matchId: String,
    val homeTeamBet: Int,
    val awayTeamBet: Int,
)
