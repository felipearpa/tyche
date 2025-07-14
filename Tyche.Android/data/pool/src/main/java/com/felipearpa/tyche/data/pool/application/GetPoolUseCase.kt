package com.felipearpa.tyche.data.pool.application

import com.felipearpa.tyche.data.pool.domain.PoolRepository
import javax.inject.Inject

class GetPoolUseCase @Inject internal constructor(private val poolRepository: PoolRepository) {
    suspend fun execute(poolId: String) =
        poolRepository.getPool(id = poolId)
}
