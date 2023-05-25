package com.felipearpa.bet.ui

import com.felipearpa.bet.domain.PoolGamblerBet

fun PoolGamblerBet.toModel() =
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