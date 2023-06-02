package com.felipearpa.tyche.bet.ui

import com.felipearpa.tyche.bet.domain.PoolGamblerBet

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