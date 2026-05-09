package com.felipearpa.tyche.data.pool.domain

internal interface PoolRepository {
    suspend fun getPool(id: String): Result<Pool>
    suspend fun createPool(createPoolInput: CreatePoolInput): Result<CreatePoolOutput>
    suspend fun joinPool(joinPoolInput: JoinPoolInput): Result<Unit>
    suspend fun deletePool(poolId: String, gamblerId: String): Result<Unit>
}
