package com.felipearpa.tyche.bet.domain

import com.felipearpa.tyche.core.type.BetScore
import com.felipearpa.tyche.core.type.Ulid

data class Bet(
    val poolId: Ulid,
    val gamblerId: Ulid,
    val matchId: Ulid,
    val homeTeamBet: BetScore,
    val awayTeamBet: BetScore
)