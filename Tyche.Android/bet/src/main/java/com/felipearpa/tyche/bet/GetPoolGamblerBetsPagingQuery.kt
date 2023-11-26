package com.felipearpa.tyche.bet

import com.felipearpa.data.bet.application.GetPoolGamblerBetsUseCase

suspend fun getPoolGamblerBetsPagingQuery(
    next: String?,
    poolId: String,
    gamblerId: String,
    search: () -> String?,
    getPoolGamblerBetsUseCase: GetPoolGamblerBetsUseCase
) = getPoolGamblerBetsUseCase.execute(
    poolId = poolId,
    gamblerId = gamblerId,
    next = next,
    searchText = search()
).map { page -> page.map { gamblerPool -> gamblerPool.toPoolGamblerBetModel() } }