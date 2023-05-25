package com.felipearpa.bet.domain

fun Bet.toRequest() =
    BetRequest(
        poolId = this.poolId.value,
        gamblerId = this.gamblerId.value,
        matchId = this.matchId.value,
        homeTeamBet = this.homeTeamBet.value,
        awayTeamBet = this.awayTeamBet.value
    )