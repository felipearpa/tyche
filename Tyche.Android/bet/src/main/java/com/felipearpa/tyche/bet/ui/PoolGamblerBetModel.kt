package com.felipearpa.tyche.bet.ui

import com.felipearpa.tyche.bet.domain.PoolGamblerBet
import com.felipearpa.tyche.core.type.TeamScore
import java.time.LocalDateTime

data class PoolGamblerBetModel(
    val poolId: String,
    val gamblerId: String,
    val matchId: String,
    val homeTeamId: String,
    val homeTeamName: String,
    val awayTeamId: String,
    val awayTeamName: String,
    val matchScore: TeamScore<Int>?,
    var betScore: TeamScore<Int>?,
    val score: Int?,
    val matchDateTime: LocalDateTime
) {
    var isLockEnforced = false

    fun isLocked() = isLockEnforced || LocalDateTime.now() >= matchDateTime
}

fun PoolGamblerBet.toPoolGamblerBetModel() =
    PoolGamblerBetModel(
        poolId = this.poolId,
        gamblerId = this.gamblerId,
        matchId = this.matchId,
        homeTeamId = this.homeTeamId,
        homeTeamName = this.homeTeamName,
        awayTeamId = this.awayTeamId,
        awayTeamName = this.awayTeamName,
        matchScore = this.matchScore,
        betScore = this.betScore,
        score = this.score,
        matchDateTime = this.matchDateTime
    )