package com.felipearpa.tyche.pool.domain

interface PoolRepository {

    suspend fun getPool(poolId: String): Result<Pool>
}