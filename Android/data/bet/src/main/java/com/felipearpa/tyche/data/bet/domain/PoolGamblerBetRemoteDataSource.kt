package com.felipearpa.tyche.data.bet.domain

import com.felipearpa.tyche.core.paging.CursorPage
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

internal interface PoolGamblerBetRemoteDataSource {
    @GET("/pools/{poolId}/gamblers/{gamblerId}/bets/pending")
    suspend fun getPendingPoolGamblerBets(
        @Path("poolId") poolId: String,
        @Path("gamblerId") gamblerId: String,
        @Query("next") next: String? = null,
        @Query("searchText") searchText: String? = null,
    ): CursorPage<PoolGamblerBetResponse>

    @GET("/pools/{poolId}/gamblers/{gamblerId}/bets/finished")
    suspend fun getFinishedPoolGamblerBets(
        @Path("poolId") poolId: String,
        @Path("gamblerId") gamblerId: String,
        @Query("next") next: String? = null,
        @Query("searchText") searchText: String? = null,
    ): CursorPage<PoolGamblerBetResponse>

    @PATCH("bets")
    suspend fun bet(@Body betRequest: BetRequest): PoolGamblerBetResponse
}
