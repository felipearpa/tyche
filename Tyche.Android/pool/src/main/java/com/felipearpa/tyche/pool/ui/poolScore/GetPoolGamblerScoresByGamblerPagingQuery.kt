package com.felipearpa.tyche.pool.ui.poolScore

import com.felipearpa.tyche.pool.application.GetPoolGamblerScoresByGamblerUseCase
import com.felipearpa.tyche.pool.ui.toModel

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