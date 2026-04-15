package com.felipearpa.tyche.data.bet.application

import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository

class GetFinishedPoolGamblerBets(private val poolGamblerBetRepository: PoolGamblerBetRepository) {
    suspend fun execute(
        poolId: String,
        gamblerId: String,
        next: String? = null,
        searchText: String? = null,
    ) =
        poolGamblerBetRepository.getFinishedPoolGamblerBets(
            poolId = poolId,
            gamblerId = gamblerId,
            next = next,
            searchText = searchText,
        )
}
