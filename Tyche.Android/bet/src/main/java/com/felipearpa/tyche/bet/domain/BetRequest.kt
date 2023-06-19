package com.felipearpa.tyche.bet.domain

data class BetRequest(
    val poolId: String,
    val gamblerId: String,
    val matchId: String,
    val homeTeamBet: Int,
    val awayTeamBet: Int
)

fun Bet.toBetRequest() =
    BetRequest(
        poolId = this.poolId.value,
        gamblerId = this.gamblerId.value,
        matchId = this.matchId.value,
        homeTeamBet = this.homeTeamBet.value,
        awayTeamBet = this.awayTeamBet.value
    )