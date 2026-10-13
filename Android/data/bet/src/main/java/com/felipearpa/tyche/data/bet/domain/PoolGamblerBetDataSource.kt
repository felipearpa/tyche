package com.felipearpa.tyche.data.bet.domain

import com.felipearpa.tyche.core.paging.CursorPage

internal interface PoolGamblerBetDataSource {
    suspend fun getPendingPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String? = null,
        searchText: String? = null,
    ): CursorPage<PoolGamblerBetResponse>

    /** Throws [KickoffGroupFeedException] for the grouped feed's own error responses. */
    suspend fun getFinishedPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String? = null,
    ): CursorPage<KickoffGroupResponse>

    suspend fun getLivePoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String? = null,
        searchText: String? = null,
    ): CursorPage<PoolGamblerBetResponse>

    /**
     * The ranked participant page. Throws [RankedMatchBetsException] for the ranked mode's own
     * error responses.
     */
    suspend fun getRankedPoolMatchGamblerBets(
        poolId: String,
        matchId: String,
        next: String? = null,
    ): CursorPage<PoolGamblerBetResponse>

    suspend fun getPoolGamblerBet(
        poolId: String,
        gamblerId: String,
        matchId: String,
    ): PoolGamblerBetResponse

    /** Throws [KickoffGroupFeedException] for the grouped feed's own error responses. */
    suspend fun getGamblerBetsTimeline(
        poolId: String,
        gamblerId: String,
        next: String? = null,
    ): CursorPage<KickoffGroupResponse>

    suspend fun bet(betRequest: BetRequest): PoolGamblerBetResponse
}
