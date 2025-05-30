package com.felipearpa.tyche.bet.pending

import com.felipearpa.tyche.data.bet.application.GetPendingPoolGamblerBetsUseCase
import com.felipearpa.tyche.bet.toPoolGamblerBetModel
import com.felipearpa.tyche.core.paging.map

suspend fun getPendingBetsPagingQuery(
    next: String?,
    poolId: String,
    gamblerId: String,
    search: () -> String?,
    getPendingPoolGamblerBetsUseCase: GetPendingPoolGamblerBetsUseCase,
) = getPendingPoolGamblerBetsUseCase.execute(
    poolId = poolId,
    gamblerId = gamblerId,
    next = next,
    searchText = search(),
).map { page -> page.map { gamblerPool -> gamblerPool.toPoolGamblerBetModel() } }
