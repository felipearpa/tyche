package com.felipearpa.tyche.data.pool.domain

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface PoolRemoteDataSource {
    @GET("pools/{poolId}")
    suspend fun getPool(@Path("poolId") id: String): PoolResponse

    @POST("pools")
    suspend fun createPool(@Body createPoolRequest: CreatePoolRequest): CreatePoolResponse

    @POST("pools/{poolId}")
    suspend fun joinPool(@Path("poolId") poolId: String, @Body joinPoolRequest: JoinPoolRequest)
}
