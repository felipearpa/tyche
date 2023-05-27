package com.felipearpa.bet.domain

import com.felipearpa.core.paging.CursorPage
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface PoolGamblerBetRemoteDataSource {

    @GET("/pool/{poolId}/gambler/{gamblerId}/bets")
    suspend fun getPoolGamblerBets(
        @Path("poolId") poolId: String,
        @Path("gamblerId") gamblerId: String,
        @Query("next") next: String? = null,
        @Query("searchText") searchText: String? = null
    ): CursorPage<PoolGamblerBetResponse>

    @PATCH("bet")
    suspend fun bet(@Body betRequest: BetRequest): PoolGamblerBetResponse
}