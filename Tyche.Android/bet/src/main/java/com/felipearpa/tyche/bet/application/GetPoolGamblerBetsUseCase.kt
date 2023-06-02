package com.felipearpa.tyche.bet.application

import com.felipearpa.tyche.bet.domain.PoolGamblerBetRepository
import javax.inject.Inject

class GetPoolGamblerBetsUseCase @Inject constructor(private val poolGamblerBetRepository: PoolGamblerBetRepository) {

    suspend fun execute(
        poolId: String,
        gamblerId: String,
        next: String? = null,
        searchText: String? = null
    ) =
        poolGamblerBetRepository.getPoolGamblerBets(
            poolId = poolId,
            gamblerId = gamblerId,
            next = next,
            searchText = searchText
        )
}