package com.felipearpa.tyche.bet

import com.felipearpa.tyche.core.type.TeamScore
import java.time.LocalDateTime
import java.util.UUID

data class PoolGamblerBetModel(
    val poolId: String,
    val gamblerId: String,
    val matchId: String,
    val homeTeamId: String,
    val homeTeamName: String,
    val awayTeamId: String,
    val awayTeamName: String,
    val matchScore: TeamScore<Int>?,
    val betScore: TeamScore<Int>?,
    val score: Int?,
    val matchDateTime: LocalDateTime,
    val isLocked: Boolean,
    val instanceId: String = UUID.randomUUID().toString()
)

fun PoolGamblerBetModel.homeTeamMatchRawValue() =
    this.matchScore?.homeTeamValue?.toString().orEmpty()

fun PoolGamblerBetModel.awayTeamMatchRawValue() =
    this.matchScore?.awayTeamValue?.toString().orEmpty()

fun PoolGamblerBetModel.homeTeamBetRawValue() =
    this.betScore?.homeTeamValue?.toString().orEmpty()

fun PoolGamblerBetModel.awayTeamBetRawValue() =
    this.betScore?.awayTeamValue?.toString().orEmpty()
