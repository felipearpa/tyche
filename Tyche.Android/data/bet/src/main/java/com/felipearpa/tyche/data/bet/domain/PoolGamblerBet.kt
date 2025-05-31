package com.felipearpa.tyche.data.bet.domain

import com.felipearpa.tyche.core.type.TeamScore
import java.time.LocalDateTime

data class PoolGamblerBet(
    val poolId: String,
    val gamblerId: String,
    val matchId: String,
    val homeTeamId: String,
    val homeTeamName: String,
    val awayTeamId: String,
    val awayTeamName: String,
    val awayTeamScore: Int?,
    val matchScore: TeamScore<Int>?,
    val betScore: TeamScore<Int>?,
    val score: Int?,
    val matchDateTime: LocalDateTime,
    val isLocked: Boolean
)
