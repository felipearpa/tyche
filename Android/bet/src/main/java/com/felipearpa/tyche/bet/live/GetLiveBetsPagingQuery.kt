package com.felipearpa.tyche.bet.live

import com.felipearpa.tyche.bet.toPoolGamblerBetModel
import com.felipearpa.tyche.core.paging.map
import com.felipearpa.tyche.data.bet.application.GetLivePoolGamblerBets

suspend fun getLiveBetsPagingQuery(
    next: String?,
    poolId: String,
    gamblerId: String,
    search: () -> String?,
    getLivePoolGamblerBets: GetLivePoolGamblerBets,
) = getLivePoolGamblerBets.execute(
    poolId = poolId,
    gamblerId = gamblerId,
    next = next,
    searchText = search(),
).map { page -> page.map { gamblerPool -> gamblerPool.toPoolGamblerBetModel() } }
