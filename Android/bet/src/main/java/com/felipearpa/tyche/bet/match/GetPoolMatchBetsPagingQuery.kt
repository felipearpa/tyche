package com.felipearpa.tyche.bet.match

import com.felipearpa.tyche.bet.toPoolGamblerBetModel
import com.felipearpa.tyche.core.paging.map
import com.felipearpa.tyche.data.bet.application.GetPoolMatchGamblerBets

suspend fun getPoolMatchBetsPagingQuery(
    next: String?,
    poolId: String,
    matchId: String,
    getPoolMatchGamblerBets: GetPoolMatchGamblerBets,
) = getPoolMatchGamblerBets.execute(
    poolId = poolId,
    matchId = matchId,
    next = next,
).map { page -> page.map { poolGamblerBet -> poolGamblerBet.toPoolGamblerBetModel() } }
