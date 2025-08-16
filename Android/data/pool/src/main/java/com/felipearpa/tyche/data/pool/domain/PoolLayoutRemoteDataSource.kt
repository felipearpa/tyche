package com.felipearpa.tyche.data.pool.domain

import com.felipearpa.tyche.core.paging.CursorPage
import retrofit2.http.GET
import retrofit2.http.Query

internal interface PoolLayoutRemoteDataSource {
    @GET("/pool-layouts/open")
    suspend fun getOpenPoolLayouts(
        @Query("next") next: String? = null,
        @Query("searchText") searchText: String? = null,
    ): CursorPage<PoolLayoutResponse>
}
