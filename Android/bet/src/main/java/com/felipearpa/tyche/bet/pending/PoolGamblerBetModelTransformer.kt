package com.felipearpa.tyche.bet.pending

import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.awayTeamBetRawValue
import com.felipearpa.tyche.bet.homeTeamBetRawValue

fun PoolGamblerBetModel.toPartialPoolGamblerBetModel() =
    PartialPoolGamblerBetModel(
        homeTeamBet = this.homeTeamBetRawValue(),
        awayTeamBet = this.awayTeamBetRawValue(),
    )
