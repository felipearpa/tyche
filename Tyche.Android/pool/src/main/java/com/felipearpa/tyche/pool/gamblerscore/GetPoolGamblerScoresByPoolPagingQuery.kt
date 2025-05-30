package com.felipearpa.tyche.pool.gamblerscore

import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScoresByPoolUseCase
import com.felipearpa.tyche.core.paging.map
import com.felipearpa.tyche.pool.toPoolGamblerScoreModel

suspend fun getPoolGamblerScoresByPoolPagingQuery(
    next: String?,
    poolId: String,
    search: () -> String?,
    getPoolGamblerScoresByPoolUseCase: GetPoolGamblerScoresByPoolUseCase,
) =
    getPoolGamblerScoresByPoolUseCase.execute(
        poolId = poolId,
        next = next,
        searchText = search(),
    ).map { page -> page.map { gamblerPool -> gamblerPool.toPoolGamblerScoreModel() } }
