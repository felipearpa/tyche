package com.pipel.tyche.data

import com.pipel.core.Page
import retrofit2.http.GET
import retrofit2.http.Query

interface PoolLayoutRepository {

    @GET("poolLayout/findActivePoolsLayouts")
    suspend fun getPoolsLayouts(
        @Query("skip") skip: Int,
        @Query("take") take: Int,
        @Query("filter") filter: String = ""
    ): Page<PoolLayoutResponse>

}