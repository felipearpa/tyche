package com.felipearpa.bet.domain

import com.felipearpa.appCore.type.BetScore
import com.felipearpa.core.type.Ulid

data class Bet(
    val poolId: Ulid,
    val gamblerId: Ulid,
    val matchId: Ulid,
    val homeTeamBet: BetScore,
    val awayTeamBet: BetScore
)