package com.felipearpa.pool.ui.gamblerScore

import com.felipearpa.pool.application.GetPoolGamblerScoresByPoolUseCase
import com.felipearpa.pool.ui.toModel

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
    ).map { page -> page.map { gamblerPool -> gamblerPool.toModel() } }
