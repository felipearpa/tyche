package com.felipearpa.tyche.poollayout.domain

import com.felipearpa.tyche.core.paging.CursorPage
import retrofit2.http.GET

internal interface PoolLayoutRemoteDataSource {
    @GET("/pool-layouts/opened")
    suspend fun getOpenPoolLayouts(): CursorPage<PoolLayoutResponse>
}