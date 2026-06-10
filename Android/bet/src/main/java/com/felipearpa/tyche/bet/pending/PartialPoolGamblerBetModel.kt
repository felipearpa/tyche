package com.felipearpa.tyche.bet.pending

import com.felipearpa.foundation.emptyString

data class PartialPoolGamblerBetModel(
    val homeTeamBet: String,
    val awayTeamBet: String,
) {
    val isValid: Boolean
        get() = homeTeamBet.toIntOrNull() != null && awayTeamBet.toIntOrNull() != null
}

fun emptyPartialPoolGamblerBetModel() =
    PartialPoolGamblerBetModel(homeTeamBet = emptyString(), awayTeamBet = emptyString())
