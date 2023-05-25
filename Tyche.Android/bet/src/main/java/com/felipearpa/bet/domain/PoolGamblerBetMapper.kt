package com.felipearpa.bet.domain

import com.felipearpa.appCore.type.TeamScore

fun PoolGamblerBetResponse.toDomain() =
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