package com.felipearpa.tyche.bet.domain

import java.time.LocalDateTime

data class PoolGamblerBetResponse(
    val poolId: String,
    val gamblerId: String,
    val matchId: String,
    val homeTeamId: String,
    val homeTeamName: String,
    val homeTeamScore: Int?,
    val homeTeamBet: Int?,
    val awayTeamId: String,
    val awayTeamName: String,
    val awayTeamScore: Int?,
    val awayTeamBet: Int?,
    val score: Int?,
    val matchDateTime: LocalDateTime
)