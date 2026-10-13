package com.felipearpa.tyche.data.bet.domain

import com.felipearpa.tyche.core.paging.CursorPage

interface PoolGamblerBetRepository {
    suspend fun bet(bet: Bet): Result<PoolGamblerBet>

    suspend fun getPendingPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String? = null,
        searchText: String? = null
    ): Result<CursorPage<PoolGamblerBet>>

    /** See `GetFinishedPoolGamblerBets`. */
    suspend fun getFinishedPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String? = null,
    ): Result<CursorPage<KickoffGroup>>

    suspend fun getLivePoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String? = null,
        searchText: String? = null
    ): Result<CursorPage<PoolGamblerBet>>

    /** See `GetRankedPoolMatchGamblerBets`. */
    suspend fun getRankedPoolMatchGamblerBets(
        poolId: String,
        matchId: String,
        next: String? = null,
    ): Result<CursorPage<PoolGamblerBet>>

    suspend fun getPoolGamblerBet(
        poolId: String,
        gamblerId: String,
        matchId: String,
    ): Result<PoolGamblerBet>

    /** See `GetGamblerBetsTimeline`. */
    suspend fun getGamblerBetsTimeline(
        poolId: String,
        gamblerId: String,
        next: String? = null,
    ): Result<CursorPage<KickoffGroup>>
}
