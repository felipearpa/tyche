package com.felipearpa.tyche.bet

import com.felipearpa.tyche.data.bet.domain.PoolGamblerBet

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
        matchDateTime = this.matchDateTime,
        isLocked = this.isLocked
    )
