package com.felipearpa.data.pool.domain

import com.felipearpa.tyche.core.paging.CursorPage
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface PoolGamblerScoreRemoteDataSource {
    @GET("/gamblers/{gamblerId}/pools")
    suspend fun getPoolGamblerScoresByGambler(
        @Path("gamblerId") gamblerId: String,
        @Query("next") next: String? = null,
        @Query("searchText") searchText: String? = null
    ): CursorPage<PoolGamblerScoreResponse>

    @GET("/pools/{poolId}/gamblers")
    suspend fun getPoolGamblerScoresByPool(
        @Path("poolId") poolId: String,
        @Query("next") next: String? = null,
        @Query("searchText") searchText: String? = null
    ): CursorPage<PoolGamblerScoreResponse>

    @GET("/pools/{poolId}/gamblers/{gamblerId}")
    suspend fun getPoolGamblerScore(
        @Path("poolId") poolId: String,
        @Path("gamblerId") gamblerId: String
    ): PoolGamblerScoreResponse
}