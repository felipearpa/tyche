package com.felipearpa.data.pool.domain

import retrofit2.http.GET
import retrofit2.http.Path

internal interface PoolRemoteDataSource {
    @GET("/pool/{poolId}")
    suspend fun getPool(@Path("poolId") poolId: String): PoolResponse
}