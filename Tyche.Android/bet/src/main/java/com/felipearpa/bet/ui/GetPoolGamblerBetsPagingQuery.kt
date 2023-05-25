package com.felipearpa.bet.ui

import com.felipearpa.bet.application.GetPoolGamblerBetsUseCase

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
).map { page -> page.map { gamblerPool -> gamblerPool.toModel() } }