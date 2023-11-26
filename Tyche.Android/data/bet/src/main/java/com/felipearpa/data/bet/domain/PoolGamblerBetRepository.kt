package com.felipearpa.data.bet.domain

import com.felipearpa.tyche.core.paging.CursorPage

interface PoolGamblerBetRepository {
    suspend fun bet(bet: Bet): Result<PoolGamblerBet>

    suspend fun getPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String? = null,
        searchText: String? = null
    ): Result<CursorPage<PoolGamblerBet>>
}