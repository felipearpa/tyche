package com.felipearpa.tyche.data.pool.domain

import com.felipearpa.tyche.core.paging.CursorPage

internal interface PoolGamblerScoreDataSource {
    suspend fun getPoolGamblerScoresByGambler(
        gamblerId: String,
        next: String? = null,
        searchText: String? = null,
    ): CursorPage<PoolGamblerScoreResponse>

    suspend fun getPoolGamblerScoresByPool(
        poolId: String,
        next: String? = null,
        searchText: String? = null,
    ): CursorPage<PoolGamblerScoreResponse>

    suspend fun getPoolGamblerScore(
        poolId: String,
        gamblerId: String,
    ): PoolGamblerScoreResponse
}
