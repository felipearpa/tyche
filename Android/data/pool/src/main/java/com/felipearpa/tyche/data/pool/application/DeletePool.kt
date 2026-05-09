package com.felipearpa.tyche.data.pool.application

import com.felipearpa.tyche.data.pool.domain.PoolRepository

class DeletePool internal constructor(private val poolRepository: PoolRepository) {
    suspend fun execute(poolId: String, gamblerId: String) =
        poolRepository.deletePool(poolId = poolId, gamblerId = gamblerId)
}
