package com.felipearpa.tyche.pool.domain

import retrofit2.http.GET
import retrofit2.http.Path

interface PoolRemoteDataSource {

    @GET("/pool/{poolId}")
    suspend fun getPool(@Path("poolId") poolId: String): PoolResponse
}