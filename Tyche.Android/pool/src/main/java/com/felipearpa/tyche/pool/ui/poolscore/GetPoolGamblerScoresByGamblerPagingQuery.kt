package com.felipearpa.tyche.pool.ui.poolscore

import com.felipearpa.tyche.pool.application.GetPoolGamblerScoresByGamblerUseCase
import com.felipearpa.tyche.pool.ui.toPoolGamblerScoreModel

suspend fun getPoolGamblerScoresByGamblerPagingQuery(
    next: String?,
    gamblerId: String,
    search: () -> String?,
    getPoolGamblerScoresByGamblerUseCase: GetPoolGamblerScoresByGamblerUseCase
) = getPoolGamblerScoresByGamblerUseCase.execute(
    gamblerId = gamblerId,
    next = next,
    searchText = search()
).map { page -> page.map { gamblerPool -> gamblerPool.toPoolGamblerScoreModel() } }