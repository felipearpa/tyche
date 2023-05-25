package com.felipearpa.bet.domain

import com.felipearpa.core.paging.CursorPage
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
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

    @PUT("bet")
    suspend fun bet(@Body betRequest: BetRequest): PoolGamblerBetResponse
}