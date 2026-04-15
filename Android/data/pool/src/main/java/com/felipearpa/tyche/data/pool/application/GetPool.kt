package com.felipearpa.tyche.data.pool.application

import com.felipearpa.tyche.data.pool.domain.PoolRepository

class GetPool internal constructor(private val poolRepository: PoolRepository) {
    suspend fun execute(poolId: String) =
        poolRepository.getPool(id = poolId)
}
