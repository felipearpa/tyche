package com.pipel.tyche.poolGambler.data

import com.pipel.core.CursorPage
import retrofit2.http.GET
import retrofit2.http.Query

interface PoolGamblerRepository {

    @GET("pool/findPoolsGamblers")
    suspend fun getPoolsGamblers(
        @Query("poolId") pooId: String,
        @Query("nextToken") nextToken: String?,
        @Query("filterText") filterText: String?
    ): CursorPage<PoolGamblerResponse>

}