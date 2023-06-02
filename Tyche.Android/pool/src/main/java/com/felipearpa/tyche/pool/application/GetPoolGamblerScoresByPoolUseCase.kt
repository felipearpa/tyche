package com.felipearpa.tyche.pool.application

import com.felipearpa.tyche.pool.domain.PoolGamblerScoreRepository
import javax.inject.Inject

class GetPoolGamblerScoresByPoolUseCase
@Inject constructor(private val poolGamblerScoreRepository: PoolGamblerScoreRepository) {

    suspend fun execute(poolId: String, next: String? = null, searchText: String? = null) =
        poolGamblerScoreRepository.getPoolGamblerScoresByPool(
            poolId = poolId,
            next = next,
            searchText = searchText
        )
}