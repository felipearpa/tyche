package com.felipearpa.data.pool.domain

interface PoolRepository {
    suspend fun getPool(poolId: String): Result<Pool>
}