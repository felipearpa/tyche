package com.felipearpa.tyche.data.pool.domain

internal interface PoolRepository {
    suspend fun createPool(createPoolInput: CreatePoolInput): Result<Pool>
}
