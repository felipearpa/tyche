package com.felipearpa.tyche.data.pool.domain

import retrofit2.http.Body
import retrofit2.http.POST

internal interface PoolRemoteDataSource {
    @POST("pools")
    suspend fun createPool(@Body createPoolRequest: CreatePoolRequest): PoolResponse
}
