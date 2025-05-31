package com.felipearpa.tyche.pool.poolscore

import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScoresByGamblerUseCase
import com.felipearpa.tyche.core.paging.map
import com.felipearpa.tyche.pool.toPoolGamblerScoreModel

suspend fun getPoolGamblerScoresByGamblerPagingQuery(
    next: String?,
    gamblerId: String,
    search: () -> String?,
    getPoolGamblerScoresByGamblerUseCase: GetPoolGamblerScoresByGamblerUseCase,
) = getPoolGamblerScoresByGamblerUseCase.execute(
    gamblerId = gamblerId,
    next = next,
    searchText = search(),
).map { page -> page.map { poolGamblerScore -> poolGamblerScore.toPoolGamblerScoreModel() } }
