package com.felipearpa.data.bet.application

import com.felipearpa.data.bet.domain.PoolGamblerBetRepository
import javax.inject.Inject

class GetPendingPoolGamblerBetsUseCase @Inject constructor(private val poolGamblerBetRepository: PoolGamblerBetRepository) {
    suspend fun execute(
        poolId: String,
        gamblerId: String,
        next: String? = null,
        searchText: String? = null
    ) =
        poolGamblerBetRepository.getPendingPoolGamblerBets(
            poolId = poolId,
            gamblerId = gamblerId,
            next = next,
            searchText = searchText
        )
}