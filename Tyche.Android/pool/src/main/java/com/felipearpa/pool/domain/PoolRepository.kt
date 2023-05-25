package com.felipearpa.pool.domain

interface PoolRepository {

    suspend fun getPool(poolId: String): Result<Pool>
}