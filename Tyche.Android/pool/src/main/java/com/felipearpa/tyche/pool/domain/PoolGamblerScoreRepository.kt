package com.felipearpa.tyche.pool.domain

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
}