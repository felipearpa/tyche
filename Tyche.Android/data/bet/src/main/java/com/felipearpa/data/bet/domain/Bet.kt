package com.felipearpa.data.bet.domain

import com.felipearpa.tyche.core.type.BetScore

data class Bet(
    val poolId: String,
    val gamblerId: String,
    val matchId: String,
    val homeTeamBet: BetScore,
    val awayTeamBet: BetScore
)