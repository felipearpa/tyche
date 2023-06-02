package com.felipearpa.tyche.pool.domain

import com.felipearpa.tyche.core.paging.CursorPage
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PoolGamblerScoreRemoteDataSource {

    @GET("/gambler/{gamblerId}/pools")
    suspend fun getPoolGamblerScoresByGambler(
        @Path("gamblerId") gamblerId: String,
        @Query("next") next: String? = null,
        @Query("searchText") searchText: String? = null
    ): CursorPage<PoolGamblerScoreResponse>


    @GET("/pool/{poolId}/gamblers")
    suspend fun getPoolGamblerScoreByPool(
        @Path("poolId") poolId: String,
        @Query("next") next: String? = null,
        @Query("searchText") searchText: String? = null
    ): CursorPage<PoolGamblerScoreResponse>
}