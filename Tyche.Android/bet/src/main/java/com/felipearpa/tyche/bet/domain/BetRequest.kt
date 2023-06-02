package com.felipearpa.tyche.bet.domain

data class BetRequest(
    val poolId: String,
    val gamblerId: String,
    val matchId: String,
    val homeTeamBet: Int,
    val awayTeamBet: Int
)