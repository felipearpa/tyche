package com.felipearpa.data.pool.application

import com.felipearpa.data.pool.domain.PoolGamblerScoreRepository
import javax.inject.Inject

class GetPoolGamblerScoreUseCase @Inject constructor(private val poolGamblerScoreRepository: PoolGamblerScoreRepository) {
    suspend fun execute(poolId: String, gamblerId: String) =
        poolGamblerScoreRepository.getPoolGamblerScore(poolId = poolId, gamblerId = gamblerId)
}