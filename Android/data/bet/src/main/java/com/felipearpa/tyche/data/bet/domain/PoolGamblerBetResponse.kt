package com.felipearpa.tyche.data.bet.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

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
