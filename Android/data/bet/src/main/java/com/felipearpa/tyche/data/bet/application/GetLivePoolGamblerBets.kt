package com.felipearpa.tyche.data.bet.application

import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository

class GetLivePoolGamblerBets(private val poolGamblerBetRepository: PoolGamblerBetRepository) {
    suspend fun execute(
        poolId: String,
        gamblerId: String,
        next: String? = null,
        searchText: String? = null,
    ) =
        poolGamblerBetRepository.getLivePoolGamblerBets(
            poolId = poolId,
            gamblerId = gamblerId,
            next = next,
            searchText = searchText,
        )
}
