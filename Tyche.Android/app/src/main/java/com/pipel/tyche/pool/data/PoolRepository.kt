package com.pipel.tyche.pool.data

import com.pipel.core.CursorPage
import retrofit2.http.GET
import retrofit2.http.Query

interface PoolRepository {

    @GET("pool/findPools")
    suspend fun getPools(
        @Query("poolLayoutId") pooLayoutId: String,
        @Query("nextToken") nextToken: String?,
        @Query("filterText") filterText: String?
    ): CursorPage<PoolResponse>

}