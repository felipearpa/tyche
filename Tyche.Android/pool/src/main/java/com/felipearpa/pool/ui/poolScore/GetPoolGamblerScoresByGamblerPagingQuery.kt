package com.felipearpa.pool.ui.poolScore

import com.felipearpa.pool.application.GetPoolGamblerScoresByGamblerUseCase
import com.felipearpa.pool.ui.toModel

suspend fun getPoolGamblerScoresByGamblerPagingQuery(
    next: String?,
    gamblerId: String,
    search: () -> String?,
    getPoolGamblerScoresByGamblerUseCase: GetPoolGamblerScoresByGamblerUseCase
) = getPoolGamblerScoresByGamblerUseCase.execute(
    gamblerId = gamblerId,
    next = next,
    searchText = search()
).map { page -> page.map { gamblerPool -> gamblerPool.toModel() } }