package com.felipearpa.data.pool.domain

import com.felipearpa.tyche.core.paging.CursorPage

interface PoolGamblerScoreRepository {
    suspend fun getPoolGamblerScoresByGambler(
        gamblerId: String,
        next: String? = null,
        searchText: String? = null
    ): Result<CursorPage<PoolGamblerScore>>

    suspend fun getPoolGamblerScoresByPool(
        poolId: String,
        next: String? = null,
        searchText: String? = null
    ): Result<CursorPage<PoolGamblerScore>>

    suspend fun getPoolGamblerScore(poolId: String, gamblerId: String): Result<PoolGamblerScore>
}