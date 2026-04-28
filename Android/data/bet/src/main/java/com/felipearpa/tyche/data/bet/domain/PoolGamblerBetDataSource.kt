package com.felipearpa.tyche.data.bet.domain

import com.felipearpa.tyche.core.paging.CursorPage

internal interface PoolGamblerBetDataSource {
    suspend fun getPendingPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String? = null,
        searchText: String? = null,
    ): CursorPage<PoolGamblerBetResponse>

    suspend fun getFinishedPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String? = null,
        searchText: String? = null,
    ): CursorPage<PoolGamblerBetResponse>

    suspend fun getLivePoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String? = null,
        searchText: String? = null,
    ): CursorPage<PoolGamblerBetResponse>

    suspend fun getPoolMatchGamblerBets(
        poolId: String,
        matchId: String,
        next: String? = null,
    ): CursorPage<PoolGamblerBetResponse>

    suspend fun bet(betRequest: BetRequest): PoolGamblerBetResponse
}
