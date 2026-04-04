package com.felipearpa.tyche.data.pool.domain

internal interface PoolDataSource {
    suspend fun getPool(id: String): PoolResponse
    suspend fun createPool(createPoolRequest: CreatePoolRequest): CreatePoolResponse
    suspend fun joinPool(poolId: String, joinPoolRequest: JoinPoolRequest)
}
