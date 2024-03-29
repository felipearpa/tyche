package com.felipearpa.data.pool.application

import com.felipearpa.data.pool.domain.PoolRepository
import javax.inject.Inject

class GetPoolUseCase @Inject constructor(private val poolRepository: PoolRepository) {
    suspend fun execute(poolId: String) =
        poolRepository.getPool(poolId = poolId)
}