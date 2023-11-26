package com.felipearpa.data.bet.domain

internal fun Bet.toBetRequest() =
    BetRequest(
        poolId = this.poolId,
        gamblerId = this.gamblerId,
        matchId = this.matchId,
        homeTeamBet = this.homeTeamBet.value,
        awayTeamBet = this.awayTeamBet.value
    )