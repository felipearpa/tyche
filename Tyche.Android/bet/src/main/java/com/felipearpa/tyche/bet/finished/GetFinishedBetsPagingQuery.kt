package com.felipearpa.tyche.bet.finished

import com.felipearpa.tyche.data.bet.application.GetFinishedPoolGamblerBetsUseCase
import com.felipearpa.tyche.bet.toPoolGamblerBetModel
import com.felipearpa.tyche.core.paging.map

suspend fun getFinishedBetsPagingQuery(
    next: String?,
    poolId: String,
    gamblerId: String,
    search: () -> String?,
    getFinishedPoolGamblerBetsUseCase: GetFinishedPoolGamblerBetsUseCase,
) = getFinishedPoolGamblerBetsUseCase.execute(
    poolId = poolId,
    gamblerId = gamblerId,
    next = next,
    searchText = search(),
).map { page -> page.map { gamblerPool -> gamblerPool.toPoolGamblerBetModel() } }
