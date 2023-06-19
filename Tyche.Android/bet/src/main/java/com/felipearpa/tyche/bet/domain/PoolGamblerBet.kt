package com.felipearpa.tyche.bet.domain

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
    val matchDateTime: LocalDateTime
)

fun PoolGamblerBetResponse.toPoolGamblerBet() =
    PoolGamblerBet(
        poolId = this.poolId,
        gamblerId = gamblerId,
        matchId = this.matchId,
        homeTeamId = this.homeTeamId,
        homeTeamName = this.homeTeamName,
        awayTeamId = this.awayTeamId,
        awayTeamName = this.awayTeamName,
        awayTeamScore = this.awayTeamScore,
        matchScore = if (this.homeTeamScore == null || this.awayTeamScore == null) null else TeamScore(
            homeTeamValue = this.homeTeamScore,
            awayTeamValue = this.awayTeamScore
        ),
        betScore = if (this.homeTeamBet == null || this.awayTeamBet == null) null else TeamScore(
            homeTeamValue = this.homeTeamBet,
            awayTeamValue = this.awayTeamBet
        ),
        score = this.score,
        matchDateTime = this.matchDateTime
    )