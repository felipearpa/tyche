package com.felipearpa.tyche.pool.ui.gamblerScore

import com.felipearpa.tyche.pool.application.GetPoolGamblerScoresByPoolUseCase
import com.felipearpa.tyche.pool.ui.toPoolGamblerScoreModel

suspend fun getPoolGamblerScoresByPoolPagingQuery(
    next: String?,
    poolId: String,
    search: () -> String?,
    getPoolGamblerScoresByPoolUseCase: GetPoolGamblerScoresByPoolUseCase
) =
    getPoolGamblerScoresByPoolUseCase.execute(
        poolId = poolId,
        next = next,
        searchText = search()
    ).map { page -> page.map { gamblerPool -> gamblerPool.toPoolGamblerScoreModel() } }
