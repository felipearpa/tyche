package com.felipearpa.tyche.data.pool.application

import com.felipearpa.tyche.data.pool.domain.PoolGamblerScoreRepository

class GetPoolGamblerScore(private val poolGamblerScoreRepository: PoolGamblerScoreRepository) {
    suspend fun execute(poolId: String, gamblerId: String) =
        poolGamblerScoreRepository.getPoolGamblerScore(poolId = poolId, gamblerId = gamblerId)
}
