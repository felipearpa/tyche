package com.felipearpa.tyche.bet

import com.felipearpa.tyche.core.emptyString

data class PartialPoolGamblerBetModel(
    val homeTeamBet: String,
    val awayTeamBet: String
)

fun emptyPartialPoolGamblerBetModel() =
    PartialPoolGamblerBetModel(homeTeamBet = emptyString(), awayTeamBet = emptyString())