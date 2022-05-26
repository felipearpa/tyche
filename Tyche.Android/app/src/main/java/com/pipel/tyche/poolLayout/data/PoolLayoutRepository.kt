package com.pipel.tyche.poolLayout.data

import com.pipel.core.CursorPage
import retrofit2.http.GET
import retrofit2.http.Query

interface PoolLayoutRepository {

    @GET("poolLayout/findActivePoolsLayouts")
    suspend fun getActivePoolsLayouts(
        @Query("nextToken") nextToken: String?,
        @Query("filterText") filterText: String?
    ): CursorPage<PoolLayoutResponse>

}