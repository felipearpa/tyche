package com.felipearpa.tyche.bet.timeline

import com.felipearpa.tyche.bet.toPoolGamblerBetModel
import com.felipearpa.tyche.core.paging.map
import com.felipearpa.tyche.data.bet.application.GetGamblerBetsTimeline

suspend fun getBetsTimelinePagingQuery(
    next: String?,
    poolId: String,
    gamblerId: String,
    getGamblerBetsTimeline: GetGamblerBetsTimeline,
) = getGamblerBetsTimeline.execute(
    poolId = poolId,
    gamblerId = gamblerId,
    next = next,
).map { page -> page.map { poolGamblerBet -> poolGamblerBet.toPoolGamblerBetModel() } }
