package com.felipearpa.pool.application

import com.felipearpa.pool.domain.PoolGamblerScoreRepository
import javax.inject.Inject

class GetPoolGamblerScoresByGamblerUseCase
@Inject constructor(private val poolGamblerScoreRepository: PoolGamblerScoreRepository) {

    suspend fun execute(gamblerId: String, next: String? = null, searchText: String? = null) =
        poolGamblerScoreRepository.getPoolGamblerScoresByGambler(
            gamblerId = gamblerId,
            next = next,
            searchText = searchText
        )
}