package com.felipearpa.data.bet.domain

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
internal data class PoolGamblerBetResponse(
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
    @Contextual val matchDateTime: LocalDateTime,
    val isLocked: Boolean,
)
