package com.felipearpa.tyche.bet.pending

import com.felipearpa.tyche.core.emptyString

data class PartialPoolGamblerBetModel(
    val homeTeamBet: String,
    val awayTeamBet: String
)

fun emptyPartialPoolGamblerBetModel() =
    PartialPoolGamblerBetModel(homeTeamBet = emptyString(), awayTeamBet = emptyString())